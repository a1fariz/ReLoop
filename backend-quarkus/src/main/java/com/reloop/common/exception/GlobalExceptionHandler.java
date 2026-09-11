package com.reloop.common.exception;

import com.reloop.common.dto.ApiErrorResponse;
import com.reloop.common.security.CorrelationContext;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Quarkus replacement for the legacy Spring @RestControllerAdvice, preserving the
 * ApiErrorResponse contract consumed by the Next.js frontend.
 */
@Provider
public class GlobalExceptionHandler {

    @Inject
    CorrelationContext correlationContext;

    @ServerExceptionMapper
    public Response handleBusinessException(BusinessException ex, @Context UriInfo uriInfo) {
        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                ex.getStatusCode(),
                ex.getCode(),
                ex.getMessage(),
                uriInfo.getRequestUri().getPath(),
                correlationId(),
                List.of()
        );
        return Response.status(ex.getStatusCode()).entity(error).build();
    }

    @ServerExceptionMapper
    public Response handleValidationException(ConstraintViolationException ex, @Context UriInfo uriInfo) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                400,
                "VALIDATION_ERROR",
                "Input validation failed",
                uriInfo.getRequestUri().getPath(),
                correlationId(),
                details
        );
        return Response.status(400).entity(error).build();
    }

    @ServerExceptionMapper
    public Response handleGenericException(Exception ex, @Context UriInfo uriInfo) {
        if (ex instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                500,
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred",
                uriInfo.getRequestUri().getPath(),
                correlationId(),
                List.of()
        );
        return Response.status(500).entity(error).build();
    }

    private String correlationId() {
        String id = correlationContext.getCorrelationId();
        return id != null ? id : UUID.randomUUID().toString();
    }
}
