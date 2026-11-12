package dev.erickmurakami.portfolio.application.dto;

import dev.erickmurakami.portfolio.domain.enums.SkillCategory;
import jakarta.validation.constraints.NotNull;

public record SkillRequest(
    @NotNull(message = "Skill name is required")
    String name,
    @NotNull(message = "Skill category is required")
    SkillCategory category
) {

}
