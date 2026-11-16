package dev.erickmurakami.portfolio.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import dev.erickmurakami.portfolio.application.dto.IngestionJobResponse;
import dev.erickmurakami.portfolio.application.services.IngestionJobService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/ingestion-jobs")
@Produces(MediaType.APPLICATION_JSON)
public class IngestionJobResource {

    @Inject
    IngestionJobService service;

    @POST
    @Path("/developers/{devId}")
    public Response create(@PathParam("devId") Long devId) {
        IngestionJobResponse response = service.create(devId, true);
        return Response.accepted(response)
                .location(URI.create("/api/v1/ingestion-jobs/" + response.jobId()))
                .build();
    }

    @GET
    @Path("/{jobId}")
    public IngestionJobResponse findById(@PathParam("jobId") UUID jobId) {
        return service.findById(jobId);
    }
}
