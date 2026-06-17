package com.instamvp.util;

import com.instamvp.model.Profile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Testa os exemplos de algoritmos usados na disciplina de Análise e Projeto de Algoritmos. */
class AlgorithmExamplesTest {

    private Profile buildProfile(String username, Long followers) {
        Profile profile = new Profile();
        profile.setUsername(username);
        profile.setFollowers(followers);
        return profile;
    }

    @Test
    void linearSearchFindsExistingUsername() {
        List<Profile> profiles = List.of(
                buildProfile("nike", 1000L),
                buildProfile("adidas", 2000L),
                buildProfile("puma", 500L)
        );

        Profile found = AlgorithmExamples.linearSearchProfileByUsername(profiles, "adidas");

        assertEquals("adidas", found.getUsername());
    }

    @Test
    void linearSearchReturnsNullWhenNotFound() {
        List<Profile> profiles = List.of(buildProfile("nike", 1000L));

        assertNull(AlgorithmExamples.linearSearchProfileByUsername(profiles, "inexistente"));
    }

    @Test
    void binarySearchFindsUsernameInSortedList() {
        List<Profile> sorted = List.of(
                buildProfile("adidas", 2000L),
                buildProfile("nike", 1000L),
                buildProfile("puma", 500L)
        ); // já em ordem alfabética

        Profile found = AlgorithmExamples.binarySearchProfileByUsername(sorted, "puma");

        assertEquals("puma", found.getUsername());
    }

    @Test
    void deduplicateKeepsFirstOccurrenceOrder() {
        List<String> usernames = List.of("nike", "adidas", "nike", "puma", "adidas");

        List<String> result = AlgorithmExamples.deduplicateUsernamesPreserveOrder(usernames);

        assertEquals(List.of("nike", "adidas", "puma"), result);
    }

    @Test
    void sortProfilesByFollowersDescOrdersHighestFirst() {
        List<Profile> profiles = List.of(
                buildProfile("puma", 500L),
                buildProfile("adidas", 2000L),
                buildProfile("nike", 1000L)
        );

        List<Profile> sorted = AlgorithmExamples.sortProfilesByFollowersDesc(profiles);

        assertEquals(List.of("adidas", "nike", "puma"),
                sorted.stream().map(Profile::getUsername).toList());
    }

    @Test
    void topKProfilesByFollowersReturnsOnlyTheKLargest() {
        List<Profile> profiles = List.of(
                buildProfile("puma", 500L),
                buildProfile("adidas", 2000L),
                buildProfile("nike", 1000L),
                buildProfile("reebok", 100L)
        );

        List<Profile> top2 = AlgorithmExamples.topKProfilesByFollowers(profiles, 2);

        assertEquals(2, top2.size());
        assertEquals(List.of("adidas", "nike"),
                top2.stream().map(Profile::getUsername).toList());
    }
}
