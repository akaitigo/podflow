package com.akaitigo.podflow.auth

import com.akaitigo.podflow.grpc.CreateEpisodeRequest
import com.akaitigo.podflow.grpc.EpisodeService
import com.akaitigo.podflow.repository.EpisodeRepository
import com.akaitigo.podflow.repository.GuestRepository
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests that the gRPC auth interceptor correctly allows authenticated calls.
 *
 * The [TestAuthClientInterceptor] automatically attaches a valid JWT to all
 * `@GrpcClient` calls, so these tests verify the "happy path" where a valid
 * token is present.
 *
 * Rejection of unauthenticated/invalid token calls is tested via REST API
 * tests since the separate gRPC server port makes direct channel tests
 * difficult in the QuarkusTest environment.
 */
@QuarkusTest
class GrpcAuthInterceptorTest {

    @GrpcClient("episode-service")
    lateinit var client: EpisodeService

    @Inject
    lateinit var episodeRepository: EpisodeRepository

    @Inject
    lateinit var guestRepository: GuestRepository

    @BeforeEach
    @Transactional
    fun cleanup() {
        episodeRepository.deleteAll()
        guestRepository.deleteAll()
    }

    @Test
    fun `grpc call with valid token succeeds`() {
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("Auth Test Episode")
            .build()

        val response = client.createEpisode(request).await().indefinitely()
        assertTrue(response.episode.id.isNotEmpty())
        assertEquals("Auth Test Episode", response.episode.title)
    }

    @Test
    fun `authenticated grpc list call returns results`() {
        // Create an episode first
        val createRequest = CreateEpisodeRequest.newBuilder()
            .setTitle("List Auth Test")
            .build()
        client.createEpisode(createRequest).await().indefinitely()

        // List should work with valid token
        val listRequest = com.akaitigo.podflow.grpc.ListEpisodesRequest
            .getDefaultInstance()
        val response = client.listEpisodes(listRequest).await().indefinitely()
        assertTrue(response.episodesList.isNotEmpty())
    }

    @Test
    fun `authenticated grpc update call succeeds`() {
        val createRequest = CreateEpisodeRequest.newBuilder()
            .setTitle("Update Auth Test")
            .build()
        val created = client.createEpisode(createRequest)
            .await().indefinitely().episode

        val updateProto = com.akaitigo.podflow.grpc.Episode.newBuilder()
            .setId(created.id)
            .setTitle("Updated Auth Test")
            .build()
        val updateRequest = com.akaitigo.podflow.grpc.UpdateEpisodeRequest
            .newBuilder()
            .setEpisode(updateProto)
            .build()

        val response = client.updateEpisode(updateRequest)
            .await().indefinitely()
        assertEquals("Updated Auth Test", response.episode.title)
    }
}
