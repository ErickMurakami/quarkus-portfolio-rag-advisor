package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

import dev.erickmurakami.portfolio.domain.enums.EvidenceStatus;
import dev.erickmurakami.portfolio.domain.models.Skill;

public record SkillEvidence(
    Skill skill,
    EvidenceStatus status,
    List<EvidenceReference> references
) {

}
