package dev.erickmurakami.portfolio.infrastructure.github;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.NonNull;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "github-api")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface GithubClient {

    @GET
    @Path("/users/{username}/repos")
    List<@NonNull GithubRepository> findRepositories(
            @PathParam("username") String username,
            @QueryParam("per_page") int perPage,
            @QueryParam("page") int page,
            @HeaderParam("Accept") String accept);

    @GET
    @Path("/repos/{owner}/{repo}/languages")
    Map<String, Long> findLanguages(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo);

    @GET
    @Path("/user/repos")
    List<@NonNull GithubRepository> findAuthenticatedRepositories(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("visibility") String visibility,
            @QueryParam("affiliation") String affiliation,
            @QueryParam("per_page") int perPage,
            @QueryParam("page") int page);

    @GET
    @Path("/repos/{owner}/{repo}/languages")
    Map<String, Long> findAuthenticatedLanguages(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/repos/{owner}/{repo}/readme")
    GithubReadmeResponse findReadme(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/repos/{owner}/{repo}")
    GithubRepository findRepository(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo,
            @HeaderParam("Authorization") String authorization);

}
