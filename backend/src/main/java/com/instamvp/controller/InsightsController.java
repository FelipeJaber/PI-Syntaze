package com.instamvp.controller;

import com.instamvp.dto.HashtagDTO;
import com.instamvp.dto.TopPostDTO;
import com.instamvp.service.InsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inteligência de conteúdo: melhores posts e hashtags em alta entre os concorrentes monitorados. */
@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    /** Melhores posts por engajamento, publicados nos últimos `days` dias (padrão: 1 = "hoje"). */
    @GetMapping("/top-posts")
    public List<TopPostDTO> getTopPosts(@RequestParam(defaultValue = "1") int days,
                                         @RequestParam(defaultValue = "10") int limit) {
        return insightsService.getTopPosts(days, limit);
    }

    /** Hashtags mais usadas pelos concorrentes monitorados nos últimos `days` dias. */
    @GetMapping("/hashtags")
    public List<HashtagDTO> getTrendingHashtags(@RequestParam(defaultValue = "7") int days,
                                                 @RequestParam(defaultValue = "15") int limit) {
        return insightsService.getTrendingHashtags(days, limit);
    }

    /** Quem postou usando uma hashtag específica (ex: /api/insights/hashtags/drop/posts) nos últimos `days` dias. */
    @GetMapping("/hashtags/{tag}/posts")
    public List<TopPostDTO> getPostsByHashtag(@PathVariable String tag,
                                               @RequestParam(defaultValue = "30") int days,
                                               @RequestParam(defaultValue = "50") int limit) {
        return insightsService.getPostsByHashtag(tag, days, limit);
    }
}
