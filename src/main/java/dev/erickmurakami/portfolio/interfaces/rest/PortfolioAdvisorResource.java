package dev.erickmurakami.portfolio.interfaces.rest;

import dev.erickmurakami.portfolio.application.dto.AdvisorRequest;
import dev.erickmurakami.portfolio.application.dto.AdvisorResponse;
import dev.erickmurakami.portfolio.application.services.PortfolioAdvisorService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/developers/{devId}/advisor")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioAdvisorResource {

    @Inject
    PortfolioAdvisorService service;

    @POST
    public AdvisorResponse advise(@PathParam("devId") Long devId, @Valid AdvisorRequest request) {
        return service.advise(devId, request.question().trim());
    }
}
