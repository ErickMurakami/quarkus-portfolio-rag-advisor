package dev.erickmurakami.portfolio.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubReadmeResponse(
        String name,
        String encoding,
        String content,
        long size,

        @JsonProperty("html_url") String htmlUrl) {

}
