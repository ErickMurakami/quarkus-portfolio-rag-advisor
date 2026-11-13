package dev.erickmurakami.portfolio.interfaces.rest;

import java.util.List;

import dev.erickmurakami.portfolio.application.dto.RepositoryInfo;
import dev.erickmurakami.portfolio.application.services.GithubRepositoryService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/github")
@Produces(MediaType.APPLICATION_JSON)
public class GithubResource {

    @Inject
    GithubRepositoryService service;

    @GET
    @Blocking
    @Path("/{username}/repositories")
    public List<RepositoryInfo> findRepositories(@PathParam("username") String username) {
        return service.findPublicRepositories(username);
    }

    @GET
    @Blocking
    @Path("/me/repositories")
    public List<RepositoryInfo> findMyRepositories() {
        return service.findOwnRepositories();
    }

}
