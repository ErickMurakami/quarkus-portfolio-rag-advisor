package dev.erickmurakami.portfolio.infrastructure.github;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepository(
        Long id,
        String name,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("private") boolean isPrivate,
        List<String> topics,
        GithubOwner owner) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubOwner(String login) {
    };
}
