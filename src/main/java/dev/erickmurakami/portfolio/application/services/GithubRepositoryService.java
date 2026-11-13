package dev.erickmurakami.portfolio.application.services;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.erickmurakami.portfolio.application.dto.RepositoryInfo;
import dev.erickmurakami.portfolio.infrastructure.github.GithubClient;
import dev.erickmurakami.portfolio.infrastructure.github.GithubRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GithubRepositoryService {

    private static final int MAX_REPOSITORIES = 50;

    @ConfigProperty(name = "github.token")
    String githubToken;

    @Inject
    @RestClient
    GithubClient client;

    public List<RepositoryInfo> findPublicRepositories(String username) {
        List<GithubRepository> repos = client.findRepositories(username, MAX_REPOSITORIES, 1,
                "application/vnd.github+json");

        return repos.stream()
                .filter(r -> !r.isPrivate())
                .map(r -> {
                    Map<String, Long> languages = client.findLanguages(username, r.name());
                    return toRepositoryInfo(r, languages);
                })
                .toList();
    }

    public List<RepositoryInfo> findOwnRepositories() {
        String authorization = "Bearer " + githubToken;

        List<GithubRepository> repos = client.findAuthenticatedRepositories(authorization, "all", "owner", 100, 1);

        return repos.stream()
                .map(repo -> {
                    Map<String, Long> languages = client.findAuthenticatedLanguages(
                            repo.owner().login(),
                            repo.name(),
                            authorization);
                    return toRepositoryInfo(repo, languages);
                })
                .toList();
    }

    private RepositoryInfo toRepositoryInfo(GithubRepository repo, Map<String, Long> languages) {

        return new RepositoryInfo(
                repo.id(),
                repo.name(),
                repo.description(),
                repo.htmlUrl(),
                repo.isPrivate(),
                languages.keySet().stream().sorted().toList(),
                repo.topics() == null ? List.of() : List.copyOf(repo.topics()));
    }
}
