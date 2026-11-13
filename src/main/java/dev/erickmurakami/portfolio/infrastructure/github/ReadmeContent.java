package dev.erickmurakami.portfolio.infrastructure.github;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;

public record ReadmeContent(
        String repository,
        String content,
        AccessScope accessScope,
        String sourceReference) {

}
