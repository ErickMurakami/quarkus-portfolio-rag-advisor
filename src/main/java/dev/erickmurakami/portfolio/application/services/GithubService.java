package dev.erickmurakami.portfolio.application.services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

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
public class GithubService {

    private static final int PAGE_SIZE = 100;
    private static final long MAX_README_BYTES = 250_000;

    @ConfigProperty(name = "github.token")
    String githubToken;

    @Inject
    @RestClient
    GithubClient client;

    public List<RepositoryInfo> findPublicRepositories(String username) {
        List<GithubRepository> repos = fetchAllPages(
                page -> client.findRepositories(username, PAGE_SIZE, page, "application/vnd.github+json"));

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

        List<GithubRepository> repos = fetchAllPages(
                page -> client.findAuthenticatedRepositories(authorization, "all", "owner", PAGE_SIZE, page));

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

    private List<GithubRepository> fetchAllPages(IntFunction<List<GithubRepository>> fetchPage) {
        List<GithubRepository> repositories = new ArrayList<>();
        int page = 1;

        while (true) {
            List<GithubRepository> currentPage = fetchPage.apply(page);

            if (currentPage == null) {
                throw new IllegalStateException("Github returned a null repository page: " + page);
            }
            repositories.addAll(currentPage);

            if (currentPage.size() < PAGE_SIZE) {
                break;
            }

            page++;
        }
        return repositories;
    }

    private ReadmeContent findReadme(String owner, String repo, String auth, boolean publicOnly) {
        GithubRepository repository = client.findRepository(owner, repo, auth);

        if (publicOnly && repository.isPrivate()) {
            throw new IllegalArgumentException("Repository is private");
        }

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

    public List<String> findRepoNames(String username, boolean includePrivate) {
        if (includePrivate) {
            validateAuthenticatedUser(username);
            String auth = "Bearer " + githubToken;

            return fetchAllPages(page -> client.findAuthenticatedRepositories(auth, "all", "owner", PAGE_SIZE, page))
                    .stream()
                    .map(g -> g.name())
                    .sorted()
                    .toList();
        }
        return fetchAllPages(page -> client.findRepositories(username, PAGE_SIZE, page, "application/vnd.github+json"))
                .stream()
                .filter(repo -> !repo.isPrivate())
                .map(g -> g.name())
                .sorted()
                .toList();
    }

    public void validateAuthenticatedUser(String username) {
        String authorization = "Bearer " + githubToken;

        String authenticatedUser = client.findOwner(authorization).login();

        if (!username.equalsIgnoreCase(authenticatedUser)) {
            throw new SecurityException("Github token does not belong to developer: " + username);
        }
    }
}
