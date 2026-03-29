package com.akaitigo.podflow

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import javax.sql.DataSource

@Path("/health")
class HealthResource @Inject constructor(
    private val dataSource: DataSource,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Response {
        return try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("SELECT 1")
                }
            }
            Response.ok(mapOf("status" to "ok", "database" to "connected")).build()
        } catch (_: Exception) {
            Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(mapOf("status" to "error", "database" to "disconnected"))
                .build()
        }
    }
}
