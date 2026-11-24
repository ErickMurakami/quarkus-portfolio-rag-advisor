package dev.erickmurakami.portfolio.interfaces.rest;

import java.net.URI;

import io.quarkus.runtime.LaunchMode;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/** Local demo boundary, not authentication or tenant isolation. */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class LocalApiFilter implements ContainerRequestFilter {
    @Inject
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) {
        if (!context.getUriInfo().getPath().replaceFirst("^/", "").startsWith("api/v1/")) return;
        if (!isLocalRequest(LaunchMode.current(), request.remoteAddress().host(),
                context.getHeaderString("Origin"), context.getUriInfo().getBaseUri())
                || "cross-site".equals(context.getHeaderString("Sec-Fetch-Site"))) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    static boolean isLocalRequest(LaunchMode mode, String remoteAddress, String origin, URI baseUri) {
        if (mode != LaunchMode.DEVELOPMENT) return false;
        if (!("127.0.0.1".equals(remoteAddress) || "::1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress))) return false;
        if (origin == null) return true; // Local CLI clients have no Origin header.
        try {
            URI supplied = URI.create(origin);
            return baseUri.getScheme().equals(supplied.getScheme())
                    && baseUri.getRawAuthority().equals(supplied.getRawAuthority())
                    && supplied.getRawUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
