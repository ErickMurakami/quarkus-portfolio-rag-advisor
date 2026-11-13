package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;

public record DeveloperProfileResponse(
        Long id,
        String fullName,
        String githubUsername,
        List<SkillResponse> skills) {
    public static DeveloperProfileResponse from(DeveloperProfile profile) {
        return new DeveloperProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getGithubUsername(),
                profile.getSkills().stream().map(SkillResponse::from).toList());
    }
}
