package dev.erickmurakami.portfolio.domain.models;

import java.util.List;

import dev.erickmurakami.portfolio.domain.enums.EvidenceStatus;

public record SkillEvidence(
    Skill skill,
    EvidenceStatus status,
    List<EvidenceReference> references
) {

}
