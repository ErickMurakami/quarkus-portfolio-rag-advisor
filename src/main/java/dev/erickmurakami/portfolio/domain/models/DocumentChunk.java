package dev.erickmurakami.portfolio.domain.models;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.DocumentSource;

public record DocumentChunk(
    Long id,
    Long devId,
    String content,
    DocumentSource source,
    AccessScope accessScope,
    String sourceReference
) {

}
