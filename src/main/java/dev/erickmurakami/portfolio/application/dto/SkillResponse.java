package dev.erickmurakami.portfolio.application.dto;

import dev.erickmurakami.portfolio.domain.enums.SkillCategory;
import dev.erickmurakami.portfolio.domain.models.Skill;

public record SkillResponse(
    String name,
    SkillCategory category
) {
    public static SkillResponse from(Skill skill){
        return new SkillResponse(skill.getName(), skill.getCategory());
    }

}
