package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record DeveloperProfileRequest(
    @NotNull(message = "Full name is required")
    String fullName,
    @NotNull(message = "GitHub username is required")
    String githubUsername,
    List<SkillRequest> skills
) {

}
