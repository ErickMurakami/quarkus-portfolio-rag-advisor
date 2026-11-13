package dev.erickmurakami.portfolio.interfaces.rest;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.dto.DeveloperProfileResponse;
import dev.erickmurakami.portfolio.application.services.DeveloperProfileService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/developers")
@Produces(MediaType.APPLICATION_JSON)
public class DeveloperProfileResource {

    @Inject
    DeveloperProfileService service;

    @POST
    @Blocking
    public DeveloperProfileResponse create(@Valid DeveloperProfileRequest request) {
        return service.create(request);
    }

    @GET
    @Path("/{id}")
    @Blocking
    public DeveloperProfileResponse getById(@PathParam("id") Long id) {
        return service.findById(id);
    }
}
