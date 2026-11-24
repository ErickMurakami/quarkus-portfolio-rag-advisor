package dev.erickmurakami.portfolio.interfaces.rest;

import java.util.Arrays;
import java.util.stream.Collectors;

import dev.erickmurakami.portfolio.domain.enums.SkillCategory;
import io.quarkus.qute.Template;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class PortfolioPageResource {
    @Inject
    Template index;

    @GET
    @Blocking
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return index.data("localAdvisor", LaunchMode.current() == LaunchMode.DEVELOPMENT)
                .data("skillCategories", Arrays.stream(SkillCategory.values()).map(Enum::name).collect(Collectors.joining(",")))
                .render();
    }
}
