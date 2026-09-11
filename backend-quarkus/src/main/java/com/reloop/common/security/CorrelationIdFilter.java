package com.reloop.common.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    private final CorrelationContext correlationContext;

    @Inject
    public CorrelationIdFilter(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            try {
                correlationId = UUID.fromString(correlationId).toString();
            } catch (IllegalArgumentException e) {
                requestContext.abortWith(Response.status(400)
                        .entity("X-Correlation-ID must be a UUID")
                        .build());
                return;
            }
        }

        correlationContext.setCorrelationId(correlationId);
        MDC.put(MDC_KEY, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationContext.getCorrelationId());
        MDC.remove(MDC_KEY);
    }
}
