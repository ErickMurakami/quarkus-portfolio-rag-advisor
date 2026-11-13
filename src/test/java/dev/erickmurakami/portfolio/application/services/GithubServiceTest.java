package dev.erickmurakami.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.erickmurakami.portfolio.application.dto.RepositoryInfo;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.infrastructure.github.GithubClient;
import dev.erickmurakami.portfolio.infrastructure.github.GithubReadmeResponse;
import dev.erickmurakami.portfolio.infrastructure.github.GithubRepository;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import jakarta.ws.rs.WebApplicationException;

@ExtendWith(MockitoExtension.class)
public class GithubServiceTest {

        private static final String OWNER = "ErickMurakami";
        private static final String REPO = "Econolyze";
        private static final String TOKEN = "test-token";
        private static final String AUTH = "Bearer " + TOKEN;

        @Mock
        GithubClient client;

        @InjectMocks
        GithubService service;

        @BeforeEach
        void setUp() {
                service.githubToken = "test-token";
        }

        @Test
        void shouldListOnlyPublicRepositories() {
                var publicRepo = repository("Econolyze", false);
                var privateRepo = repository("InternalProject", true);

                when(client.findRepositories(
                                OWNER, 100, 1, "application/vnd.github+json"))
                                .thenReturn(List.of(publicRepo, privateRepo));

                when(client.findLanguages(OWNER, "Econolyze"))
                                .thenReturn(Map.of(
                                                "Java", 1000L,
                                                "TypeScript", 500L));

                List<RepositoryInfo> result = service.findPublicRepositories(OWNER);

                assertEquals(1, result.size());
                assertEquals("Econolyze", result.getFirst().name());
                assertFalse(result.getFirst().isPrivate());
                assertEquals(
                                List.of("Java", "TypeScript"),
                                result.getFirst().languages());

                verify(client, never())
                                .findLanguages(OWNER, "InternalProject");
        }

        @Test
        void shouldListAuthenticatedRepositories() {
                var publicRepo = repository("Econolyze", false);
                var privateRepo = repository("InternalProject", true);

                when(client.findAuthenticatedRepositories(
                                AUTH, "all", "owner", 100, 1)).thenReturn(List.of(publicRepo, privateRepo));

                when(client.findAuthenticatedLanguages(
                                OWNER, "Econolyze", AUTH)).thenReturn(Map.of("Java", 1000L));

                when(client.findAuthenticatedLanguages(
                                OWNER, "InternalProject", AUTH)).thenReturn(Map.of("Kotlin", 500L));

                List<RepositoryInfo> result = service.findOwnRepositories();

                assertEquals(2, result.size());

                assertFalse(result.get(0).isPrivate());
                assertTrue(result.get(1).isPrivate());

                assertEquals(
                                List.of("Kotlin"),
                                result.get(1).languages());

                verify(client).findAuthenticatedRepositories(
                                AUTH, "all", "owner", 100, 1);
        }

        @Test
        void shouldReadPublicReadme() {
                String markdown = "# Econolyze\nBuilt with Quarkus";

                mockReadme(false, null, markdown);

                ReadmeContent result = service.findPublicReadme(OWNER, REPO);

                assertEquals(REPO, result.repository());
                assertEquals(markdown, result.content());
                assertEquals(AccessScope.PUBLIC, result.accessScope());
                assertEquals(readmeUrl(), result.sourceReference());

                verify(client).findRepository(OWNER, REPO, null);
                verify(client).findReadme(OWNER, REPO, null);
        }

        @Test
        void shouldReadPrivateReadme() {
                String markdown = "# Internal project\nUses Kotlin";

                mockReadme(true, AUTH, markdown);

                ReadmeContent result = service.findAuthenticatedReadme(OWNER, REPO);

                assertEquals(REPO, result.repository());
                assertEquals(markdown, result.content());
                assertEquals(AccessScope.PRIVATE, result.accessScope());

                verify(client).findRepository(OWNER, REPO, AUTH);
                verify(client).findReadme(OWNER, REPO, AUTH);
        }

        @Test
        void shouldReadPublicReadmeWithAuthentication() {
                mockReadme(false, AUTH, "# Public project");

                ReadmeContent result = service.findAuthenticatedReadme(OWNER, REPO);

                assertEquals(AccessScope.PUBLIC, result.accessScope());
        }

        @Test
        void shouldRejectReadmeExceedingDeclaredSize() {
                when(client.findRepository(OWNER, REPO, null))
                                .thenReturn(repository(REPO, false));

                when(client.findReadme(OWNER, REPO, null))
                                .thenReturn(new GithubReadmeResponse(
                                                "README.md",
                                                "base64",
                                                "SGVsbG8=",
                                                250_001,
                                                readmeUrl()));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findPublicReadme(OWNER, REPO));

                assertEquals(
                                "README exceeds supported size",
                                exception.getMessage());
        }

        @Test
        void shouldFetchAllPublicRepositoryPages() {
                List<GithubRepository> firstPage = java.util.stream.IntStream.range(0, 100)
                                .mapToObj(i -> repository("Repo" + i, false))
                                .toList();

                GithubRepository lastRepo = repository("LastRepo", false);

                when(client.findRepositories(
                                OWNER, 100, 1, "application/vnd.github+json")).thenReturn(firstPage);

                when(client.findRepositories(
                                OWNER, 100, 2, "application/vnd.github+json")).thenReturn(List.of(lastRepo));

                when(client.findLanguages(
                                org.mockito.ArgumentMatchers.eq(OWNER),
                                anyString())).thenReturn(Map.of("Java", 1000L));

                List<RepositoryInfo> result = service.findPublicRepositories(OWNER);

                assertEquals(101, result.size());
                assertEquals("LastRepo", result.getLast().name());

                verify(client).findRepositories(
                                OWNER, 100, 2, "application/vnd.github+json");
        }

        @Test
        void shouldFetchAllAuthenticatedRepositoryPages() {
                List<GithubRepository> firstPage = java.util.stream.IntStream.range(0, 100)
                                .mapToObj(i -> repository("Repo" + i, false))
                                .toList();

                GithubRepository privateRepo = repository("PrivateProject", true);

                when(client.findAuthenticatedRepositories(
                                AUTH, "all", "owner", 100, 1)).thenReturn(firstPage);

                when(client.findAuthenticatedRepositories(
                                AUTH, "all", "owner", 100, 2)).thenReturn(List.of(privateRepo));

                when(client.findAuthenticatedLanguages(
                                org.mockito.ArgumentMatchers.eq(OWNER),
                                anyString(),
                                org.mockito.ArgumentMatchers.eq(AUTH))).thenReturn(Map.of("Java", 1000L));

                List<RepositoryInfo> result = service.findOwnRepositories();

                assertEquals(101, result.size());
                assertTrue(result.getLast().isPrivate());

                verify(client).findAuthenticatedRepositories(
                                AUTH, "all", "owner", 100, 2);
        }

        @Test
        void shouldRejectReadmeExceedingDecodedSize() {
                byte[] largeContent = new byte[250_001];

                String encoded = Base64.getEncoder()
                                .encodeToString(largeContent);

                when(client.findRepository(OWNER, REPO, null))
                                .thenReturn(repository(REPO, false));

                when(client.findReadme(OWNER, REPO, null))
                                .thenReturn(new GithubReadmeResponse(
                                                "README.md",
                                                "base64",
                                                encoded,
                                                0,
                                                readmeUrl()));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findPublicReadme(OWNER, REPO));
        }

        @Test
        void shouldRejectUnsupportedEncoding() {
                when(client.findRepository(OWNER, REPO, null))
                                .thenReturn(repository(REPO, false));

                when(client.findReadme(OWNER, REPO, null))
                                .thenReturn(new GithubReadmeResponse(
                                                "README.md",
                                                "utf-8",
                                                "# README",
                                                8,
                                                readmeUrl()));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findPublicReadme(OWNER, REPO));

                assertEquals(
                                "Unsupported README encoding",
                                exception.getMessage());
        }

        @Test
        void shouldRejectMissingReadmeContent() {
                when(client.findRepository(OWNER, REPO, null))
                                .thenReturn(repository(REPO, false));

                when(client.findReadme(OWNER, REPO, null))
                                .thenReturn(new GithubReadmeResponse(
                                                "README.md",
                                                "base64",
                                                null,
                                                0,
                                                readmeUrl()));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findPublicReadme(OWNER, REPO));
        }

        @Test
        void shouldPropagateGithubApiError() {
                when(client.findRepository(OWNER, REPO, null))
                                .thenThrow(new WebApplicationException(404));

                assertThrows(
                                WebApplicationException.class,
                                () -> service.findPublicReadme(OWNER, REPO));
        }

        @Test
        void shouldRejectPrivateRepositoryFromPublicFlow() {
                when(client.findRepository(OWNER, REPO, null))
                                .thenReturn(repository(REPO, true));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> service.findPublicReadme(OWNER, REPO));

                assertEquals(
                                "Repository is private",
                                exception.getMessage());

                verify(client, never()).findReadme(
                                anyString(),
                                anyString(),
                                nullable(String.class));
        }

        private GithubRepository repository(
                        String name,
                        boolean isPrivate) {
                return new GithubRepository(
                                1023940245L,
                                name,
                                "Test repository",
                                "https://github.com/" + OWNER + "/" + name,
                                isPrivate,
                                List.of("java", "quarkus"),
                                new GithubRepository.GithubOwner(OWNER));
        }

        private void mockReadme(
                        boolean isPrivate,
                        String authorization,
                        String markdown) {
                byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);

                String encoded = Base64.getEncoder()
                                .encodeToString(bytes);

                when(client.findRepository(
                                OWNER, REPO, authorization)).thenReturn(repository(REPO, isPrivate));

                when(client.findReadme(
                                OWNER, REPO, authorization)).thenReturn(new GithubReadmeResponse(
                                                "README.md",
                                                "base64",
                                                encoded,
                                                bytes.length,
                                                readmeUrl()));
        }

        private String readmeUrl() {
                return "https://github.com/"
                                + OWNER + "/"
                                + REPO
                                + "/blob/main/README.md";
        }
}
