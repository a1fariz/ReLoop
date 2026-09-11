package com.reloop.common.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Compatibility shim: the gateway nginx.conf and any ops probes still target
 * the legacy Spring Boot actuator path.
 */
@Path("/actuator/health")
@ApplicationScoped
public class ActuatorHealthCompatResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String health() {
        return "{\"status\":\"UP\"}";
    }
}
