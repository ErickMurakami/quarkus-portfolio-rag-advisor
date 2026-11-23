package dev.erickmurakami.portfolio.interfaces.rest;

import java.util.List;

import dev.erickmurakami.portfolio.application.dto.AdvisorRequest;
import dev.erickmurakami.portfolio.application.dto.RetrievalResult;
import dev.erickmurakami.portfolio.application.services.PortfolioRetrievalService;
import dev.erickmurakami.portfolio.domain.enums.RetrievalMode;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/developers/{devId}/retrieval")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PortfolioRetrievalResource {

    @Inject
    PortfolioRetrievalService service;

    @POST
    public List<RetrievalResult> retrieve(@PathParam("devId") Long devId, @Valid AdvisorRequest request,
            @QueryParam("mode") @DefaultValue("FOCUSED") RetrievalMode mode) {
        return service.retrievePublic(devId, request.question(), mode)
                .stream()
                .map(RetrievalResult::toResult)
                .toList();
    }
}
