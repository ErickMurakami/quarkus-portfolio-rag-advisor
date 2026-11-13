package dev.erickmurakami.portfolio.application.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.erickmurakami.portfolio.application.dto.RepositoryInfo;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.infrastructure.github.GithubClient;
import dev.erickmurakami.portfolio.infrastructure.github.GithubReadmeResponse;
import dev.erickmurakami.portfolio.infrastructure.github.GithubRepository;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GithubRepositoryService {

    private static final int MAX_REPOSITORIES = 50;
    private static final long MAX_README_BYTES = 250_000;

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

    public ReadmeContent findPublicReadme(String owner, String repository) {
        return findReadme(owner, repository, null, true);
    }

    public ReadmeContent findAuthenticatedReadme(String owner, String repo) {
        return findReadme(owner, repo, "Bearer " + githubToken, false);
    }

    private ReadmeContent findReadme(String owner, String repo, String auth, boolean publicOnly) {
        GithubRepository repository = client.findRepository(owner, repo, auth);

        GithubReadmeResponse readme = client.findReadme(owner, repo, auth);

        if (readme.size() > MAX_README_BYTES) {
            throw new IllegalArgumentException("README exceeds supported size");
        }

        if (!"base64".equalsIgnoreCase(readme.encoding()) || readme.content() == null) {
            throw new IllegalArgumentException("Unsupported README encoding");
        }

        byte[] decoded = Base64.getMimeDecoder().decode(readme.content());

        if (decoded.length > MAX_README_BYTES) {
            throw new IllegalArgumentException("README exceeds supported size");
        }

        String content = new String(decoded, StandardCharsets.UTF_8);

        AccessScope scope = repository.isPrivate() ? AccessScope.PRIVATE : AccessScope.PUBLIC;

        return new ReadmeContent(repository.name(), content, scope, readme.htmlUrl());
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
