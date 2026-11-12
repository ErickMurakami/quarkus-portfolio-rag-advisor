package dev.erickmurakami.portfolio.domain.models;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.DocumentSource;

public record EvidenceReference(
    Long documentId,
    DocumentSource source,
    AccessScope accessScope,
    double similarityScore
) {

}
