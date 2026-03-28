package com.akaitigo.podflow

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/health")
class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Map<String, String> {
        return mapOf("status" to "ok")
    }
}
