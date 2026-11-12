package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

public record PortfolioAnalysis(
    Long devId,
    List<SkillEvidence> skillEvidences,
    List<String> recommendations
) {

}
