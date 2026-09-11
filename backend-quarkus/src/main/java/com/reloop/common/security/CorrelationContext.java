package com.reloop.common.security;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped holder for the correlation id set by {@link CorrelationIdFilter},
 * so resource methods and exception mappers can include it in the response envelope.
 */
@RequestScoped
public class CorrelationContext {
    private String correlationId;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
