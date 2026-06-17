package com.instamvp.controller;

import com.instamvp.dto.GrowthDTO;
import com.instamvp.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ranking de todos os perfis monitorados por crescimento de seguidores e engajamento. */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final ProfileService profileService;

    public LeaderboardController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<GrowthDTO> getLeaderboard(@RequestParam(defaultValue = "7") int days) {
        return profileService.computeLeaderboard(days);
    }
}
