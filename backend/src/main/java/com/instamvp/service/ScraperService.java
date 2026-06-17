package com.instamvp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instamvp.model.Post;
import com.instamvp.model.Profile;
import com.instamvp.model.ProfileSnapshot;
import com.instamvp.repository.PostRepository;
import com.instamvp.repository.ProfileRepository;
import com.instamvp.repository.ProfileSnapshotRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Best-effort scraper for public Instagram profiles using Jsoup.
 * Instagram aggressively limits anonymous/non-JS scraping, so this parses
 * whatever structured JSON (script#__NEXT_DATA__ or window._sharedData) is
 * embedded in the server-rendered page. Private profiles or pages that
 * Instagram serves a login-wall for are skipped.
 */
@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ProfileSnapshotRepository profileSnapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScraperService(ProfileRepository profileRepository, PostRepository postRepository,
                           ProfileSnapshotRepository profileSnapshotRepository) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.profileSnapshotRepository = profileSnapshotRepository;
    }

    public Map<String, String> syncUsernames(List<String> usernames) {
        Map<String, String> results = new java.util.LinkedHashMap<>();
        for (String username : usernames) {
            try {
                String status = syncOne(username.trim());
                results.put(username, status);
            } catch (Exception e) {
                log.error("Failed to scrape {}: {}", username, e.getMessage());
                results.put(username, "ERROR: " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public String syncOne(String username) throws Exception {
        Document doc = fetchDocument(username);
        JsonNode root = extractJson(doc);

        if (root == null) {
            return "SKIPPED: could not locate profile data (login wall or layout change)";
        }

        JsonNode userNode = findNodeWithField(root, "edge_followed_by");
        if (userNode == null) {
            userNode = findNodeWithField(root, "is_private");
        }
        if (userNode == null) {
            return "SKIPPED: profile JSON not found";
        }

        boolean isPrivate = userNode.path("is_private").asBoolean(false);
        if (isPrivate) {
            return "SKIPPED: private profile";
        }

        Profile profile = profileRepository.findByUsername(username).orElseGet(Profile::new);
        profile.setUsername(username);
        profile.setFullName(textOrNull(userNode, "full_name"));
        profile.setBio(textOrNull(userNode, "biography"));
        profile.setFollowers(countOf(userNode, "edge_followed_by"));
        profile.setFollowing(countOf(userNode, "edge_follow"));
        profile.setPostsCount(countOf(userNode, "edge_owner_to_timeline_media"));

        Profile saved = profileRepository.save(profile);
        recordSnapshot(saved);

        // Limite combinado com BrowserScraperService: até 100 posts, ou todos se o
        // perfil tiver menos. Via HTML/Jsoup só conseguimos o que vier nos `edges`
        // embutidos na página (sem paginação real) — tipicamente bem menos que 100.
        JsonNode mediaNode = userNode.path("edge_owner_to_timeline_media").path("edges");
        int max = 100;
        int count = 0;
        if (mediaNode.isArray()) {
            for (JsonNode edge : mediaNode) {
                if (count >= max) break;
                JsonNode node = edge.path("node");
                if (node.isMissingNode()) continue;

                String postId = textOrNull(node, "id");
                if (postId == null) continue;

                Post post = postRepository.findByProfileIdAndInstagramPostId(saved.getId(), postId)
                        .orElseGet(Post::new);
                post.setInstagramPostId(postId);
                post.setProfile(saved);
                post.setCaption(extractCaption(node));
                post.setLikes(countOf(node, "edge_liked_by") != null
                        ? countOf(node, "edge_liked_by")
                        : countOf(node, "edge_media_preview_like"));
                post.setComments(countOf(node, "edge_media_to_comment"));

                long takenAt = node.path("taken_at_timestamp").asLong(0);
                if (takenAt > 0) {
                    post.setPostDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(takenAt), ZoneId.systemDefault()));
                }

                String shortcode = textOrNull(node, "shortcode");
                post.setPostUrl(shortcode != null ? "https://www.instagram.com/p/" + shortcode + "/" : null);
                post.setImageUrl(textOrNull(node, "display_url"));

                postRepository.save(post);
                count++;
            }
        }

        return "OK: synced profile and " + count + " posts";
    }

    private Document fetchDocument(String username) throws Exception {
        String url = "https://www.instagram.com/" + username + "/";
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(15000)
                .ignoreHttpErrors(true)
                .execute();

        if (response.statusCode() == 404) {
            throw new IllegalStateException("profile not found");
        }
        return response.parse();
    }

    private JsonNode extractJson(Document doc) {
        Element nextData = doc.selectFirst("script#__NEXT_DATA__");
        if (nextData != null) {
            try {
                return objectMapper.readTree(nextData.data());
            } catch (Exception ignored) {
            }
        }

        for (Element script : doc.select("script[type=text/javascript]")) {
            String data = script.data();
            if (data.contains("window._sharedData")) {
                String json = data.replaceFirst("window\\._sharedData\\s*=\\s*", "")
                        .replaceFirst(";\\s*$", "");
                try {
                    return objectMapper.readTree(json);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /** Recursively searches a JSON tree for the first object containing the given field name. */
    private JsonNode findNodeWithField(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) return null;
        if (node.has(fieldName)) return node;

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            JsonNode result = findNodeWithField(children.next(), fieldName);
            if (result != null) return result;
        }
        return null;
    }

    private void recordSnapshot(Profile profile) {
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.setProfile(profile);
        snapshot.setFollowers(profile.getFollowers());
        snapshot.setFollowing(profile.getFollowing());
        snapshot.setPostsCount(profile.getPostsCount());
        profileSnapshotRepository.save(snapshot);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long countOf(JsonNode node, String edgeField) {
        JsonNode value = node.path(edgeField).path("count");
        return value.isMissingNode() ? null : value.asLong();
    }

    private String extractCaption(JsonNode node) {
        JsonNode edges = node.path("edge_media_to_caption").path("edges");
        if (edges.isArray() && edges.size() > 0) {
            return edges.get(0).path("node").path("text").asText(null);
        }
        return null;
    }
}
