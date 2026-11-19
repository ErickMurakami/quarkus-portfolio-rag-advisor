package dev.erickmurakami.portfolio.application.dto;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;

public record RetrievedChunk(
        Long id,
        String content,
        String sourceReference,
        AccessScope accessScope,
        double similarity) {

}
