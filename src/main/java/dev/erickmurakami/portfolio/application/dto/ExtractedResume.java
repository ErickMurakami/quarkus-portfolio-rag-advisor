package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

public record ExtractedResume(
    String fullName,
    List<SkillRequest> skills
) {

}
