package dev.erickmurakami.portfolio.interfaces.rest;

import dev.erickmurakami.portfolio.application.services.RepositoryIngestionService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/developers")
@Produces(MediaType.APPLICATION_JSON)
public class IngestionResource {

    @Inject
    RepositoryIngestionService service;

    @POST
    @Blocking
    @Path("/{devId}/repositories/{owner}/{repository}/ingest")
    public int ingestRepository(@PathParam("devId") Long devId, @PathParam("owner") String owner,
            @PathParam("repository") String repository) {
        return service.ingestPublicRepository(
                devId, owner, repository);
    }

}
