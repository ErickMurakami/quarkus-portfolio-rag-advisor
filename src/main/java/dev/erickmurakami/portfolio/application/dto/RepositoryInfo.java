package dev.erickmurakami.portfolio.application.dto;

import java.util.List;

public record RepositoryInfo(
    Long githubId,
    String name,
    String description,
    String htmlUrl,
    boolean isPrivate,
    List<String> languages,
    List<String> topics
) {

}
