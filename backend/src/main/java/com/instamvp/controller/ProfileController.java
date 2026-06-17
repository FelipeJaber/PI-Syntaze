package com.instamvp.controller;

import com.instamvp.dto.GrowthDTO;
import com.instamvp.dto.PostDTO;
import com.instamvp.dto.ProfileDTO;
import com.instamvp.service.ProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<ProfileDTO> getAll() {
        return profileService.findAll();
    }

    @GetMapping("/{id}")
    public ProfileDTO getById(@PathVariable Long id) {
        return profileService.findById(id);
    }

    @GetMapping("/{id}/posts")
    public List<PostDTO> getPosts(@PathVariable Long id) {
        return profileService.findPostsByProfileId(id);
    }

    /** Crescimento de seguidores/posts no período (padrão 7 dias). */
    @GetMapping("/{id}/growth")
    public GrowthDTO getGrowth(@PathVariable Long id, @RequestParam(defaultValue = "7") int days) {
        return profileService.computeGrowth(id, days);
    }
}
