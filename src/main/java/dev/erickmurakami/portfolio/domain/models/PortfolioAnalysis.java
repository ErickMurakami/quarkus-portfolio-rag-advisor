package dev.erickmurakami.portfolio.domain.models;

import java.util.List;

public record PortfolioAnalysis(
    Long devId,
    List<SkillEvidence> skillEvidences,
    List<String> recommendations
) {

}
