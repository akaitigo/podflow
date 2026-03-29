package com.akaitigo.podflow

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import javax.sql.DataSource

@Path("/health")
class HealthResource @Inject constructor(
    private val dataSource: DataSource,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Map<String, String> {
        val dbStatus = checkDatabase()
        val status = if (dbStatus) { "ok" } else { "degraded" }
        val dbLabel = if (dbStatus) { "connected" } else { "disconnected" }
        return mapOf("status" to status, "database" to dbLabel)
    }

    private fun checkDatabase(): Boolean =
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("SELECT 1")
                }
            }
            true
        } catch (_: Exception) {
            false
        }
}
