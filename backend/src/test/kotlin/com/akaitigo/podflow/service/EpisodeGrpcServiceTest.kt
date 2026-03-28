package com.akaitigo.podflow.service

import com.akaitigo.podflow.grpc.CreateEpisodeRequest
import com.akaitigo.podflow.grpc.DeleteEpisodeRequest
import com.akaitigo.podflow.grpc.EpisodeService
import com.akaitigo.podflow.grpc.GetEpisodeRequest
import com.akaitigo.podflow.grpc.ListEpisodesRequest
import com.akaitigo.podflow.grpc.UpdateEpisodeRequest
import com.akaitigo.podflow.model.Guest
import com.akaitigo.podflow.repository.GuestRepository
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.akaitigo.podflow.grpc.Episode as ProtoEpisode
import com.akaitigo.podflow.grpc.EpisodeStatus as ProtoEpisodeStatus

@QuarkusTest
class EpisodeGrpcServiceTest {

    @GrpcClient("episode-service")
    lateinit var client: EpisodeService

    @Inject
    lateinit var guestRepository: GuestRepository

    @Inject
    lateinit var episodeRepository: com.akaitigo.podflow.repository.EpisodeRepository

    @BeforeEach
    @Transactional
    fun cleanup() {
        episodeRepository.deleteAll()
        guestRepository.deleteAll()
    }

    @Test
    fun `createEpisode returns episode with generated id`() {
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("Test Episode")
            .setDescription("A test episode")
            .build()

        val response = client.createEpisode(request).await().indefinitely()
        val episode = response.episode

        assertNotNull(episode)
        assertTrue(episode.id.isNotEmpty())
        assertEquals("Test Episode", episode.title)
        assertEquals("A test episode", episode.description)
        assertEquals(ProtoEpisodeStatus.EPISODE_STATUS_PLANNING, episode.status)
    }

    @Test
    fun `createEpisode with blank title fails`() {
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("  ")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.createEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("Title"))
    }

    @Test
    fun `createEpisode with guest`() {
        val guest = createTestGuest()

        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("Guest Episode")
            .setGuestId(requireNotNull(guest.id).toString())
            .build()

        val response = client.createEpisode(request).await().indefinitely()
        assertEquals(requireNotNull(guest.id).toString(), response.episode.guestId)
    }

    @Test
    fun `createEpisode with nonexistent guest fails`() {
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("Guest Episode")
            .setGuestId("00000000-0000-0000-0000-000000000001")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.createEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `getEpisode returns existing episode`() {
        val created = createTestEpisode("Get Me")

        val request = GetEpisodeRequest.newBuilder()
            .setId(created.id)
            .build()

        val response = client.getEpisode(request).await().indefinitely()
        assertEquals(created.id, response.episode.id)
        assertEquals("Get Me", response.episode.title)
    }

    @Test
    fun `getEpisode with nonexistent id fails`() {
        val request = GetEpisodeRequest.newBuilder()
            .setId("00000000-0000-0000-0000-000000000099")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.getEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `getEpisode with invalid uuid fails`() {
        val request = GetEpisodeRequest.newBuilder()
            .setId("not-a-uuid")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.getEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
    }

    @Test
    fun `listEpisodes returns all episodes`() {
        createTestEpisode("Episode 1")
        createTestEpisode("Episode 2")

        val request = ListEpisodesRequest.getDefaultInstance()
        val response = client.listEpisodes(request).await().indefinitely()

        assertEquals(2, response.episodesList.size)
    }

    @Test
    fun `listEpisodes with page size returns limited results`() {
        createTestEpisode("Episode 1")
        createTestEpisode("Episode 2")
        createTestEpisode("Episode 3")

        val request = ListEpisodesRequest.newBuilder()
            .setPageSize(2)
            .build()

        val response = client.listEpisodes(request).await().indefinitely()
        assertEquals(2, response.episodesList.size)
        assertTrue(response.nextPageToken.isNotEmpty())
    }

    @Test
    fun `listEpisodes with status filter`() {
        val episode = createTestEpisode("Filtered Episode")

        val request = ListEpisodesRequest.newBuilder()
            .setStatusFilter(ProtoEpisodeStatus.EPISODE_STATUS_PLANNING)
            .build()

        val response = client.listEpisodes(request).await().indefinitely()
        assertTrue(response.episodesList.isNotEmpty())
        assertTrue(response.episodesList.all { it.status == ProtoEpisodeStatus.EPISODE_STATUS_PLANNING })
    }

    @Test
    fun `updateEpisode changes title`() {
        val created = createTestEpisode("Original Title")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setTitle("Updated Title")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val response = client.updateEpisode(request).await().indefinitely()
        assertEquals("Updated Title", response.episode.title)
    }

    @Test
    fun `updateEpisode valid status transition succeeds`() {
        val created = createTestEpisode("Status Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setStatus(ProtoEpisodeStatus.EPISODE_STATUS_RECORDING)
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val response = client.updateEpisode(request).await().indefinitely()
        assertEquals(ProtoEpisodeStatus.EPISODE_STATUS_RECORDING, response.episode.status)
    }

    @Test
    fun `updateEpisode invalid status transition fails`() {
        val created = createTestEpisode("Invalid Transition")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setStatus(ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED)
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("Invalid status transition"))
    }

    @Test
    fun `updateEpisode with nonexistent id fails`() {
        val updatedProto = ProtoEpisode.newBuilder()
            .setId("00000000-0000-0000-0000-000000000099")
            .setTitle("Ghost")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `deleteEpisode removes episode`() {
        val created = createTestEpisode("Delete Me")

        val deleteRequest = DeleteEpisodeRequest.newBuilder()
            .setId(created.id)
            .build()

        client.deleteEpisode(deleteRequest).await().indefinitely()

        val getRequest = GetEpisodeRequest.newBuilder()
            .setId(created.id)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.getEpisode(getRequest).await().indefinitely()
        }
        assertEquals(io.grpc.Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `deleteEpisode with nonexistent id fails`() {
        val request = DeleteEpisodeRequest.newBuilder()
            .setId("00000000-0000-0000-0000-000000000099")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.deleteEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.NOT_FOUND.code, exception.status.code)
    }

    @Test
    fun `deleteEpisode on published episode fails with FAILED_PRECONDITION`() {
        val created = createTestEpisode("Published No Delete")
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_RECORDING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_EDITING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_REVIEW)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED)

        val request = DeleteEpisodeRequest.newBuilder()
            .setId(created.id)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.deleteEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.FAILED_PRECONDITION.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("published"))
    }

    @Test
    fun `createEpisode with title exceeding 500 chars fails`() {
        val longTitle = "A".repeat(501)
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle(longTitle)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.createEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("500"))
    }

    @Test
    fun `updateEpisode with invalid audio_url fails`() {
        val created = createTestEpisode("Audio URL Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setAudioUrl("ftp://invalid-protocol.com/audio.mp3")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("audio_url"))
    }

    @Test
    fun `full workflow transition path`() {
        val created = createTestEpisode("Workflow Test")

        val transitions = listOf(
            ProtoEpisodeStatus.EPISODE_STATUS_GUEST_COORDINATION,
            ProtoEpisodeStatus.EPISODE_STATUS_RECORDING,
            ProtoEpisodeStatus.EPISODE_STATUS_EDITING,
            ProtoEpisodeStatus.EPISODE_STATUS_REVIEW,
            ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED,
        )

        var currentId = created.id
        transitions.forEach { targetStatus ->
            val updatedProto = ProtoEpisode.newBuilder()
                .setId(currentId)
                .setStatus(targetStatus)
                .build()

            val request = UpdateEpisodeRequest.newBuilder()
                .setEpisode(updatedProto)
                .build()

            val response = client.updateEpisode(request).await().indefinitely()
            assertEquals(targetStatus, response.episode.status)
            currentId = response.episode.id
        }
    }

    @Test
    fun `published episode cannot transition`() {
        val created = createTestEpisode("Published Lock")
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_RECORDING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_EDITING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_REVIEW)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED)

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setStatus(ProtoEpisodeStatus.EPISODE_STATUS_EDITING)
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
    }

    @Test
    fun `review to editing and back to review succeeds`() {
        val created = createTestEpisode("Review Cycle")
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_RECORDING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_EDITING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_REVIEW)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_EDITING)

        val response = transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_REVIEW)
        assertEquals(ProtoEpisodeStatus.EPISODE_STATUS_REVIEW, response.episode.status)
    }

    @Test
    fun `published episode has published_at timestamp set`() {
        val created = createTestEpisode("Publish Time")
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_RECORDING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_EDITING)
        transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_REVIEW)
        val response = transitionTo(created.id, ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED)

        assertTrue(response.episode.hasPublishedAt())
        assertTrue(response.episode.publishedAt.seconds > 0)
    }

    private fun createTestEpisode(title: String): ProtoEpisode {
        val request = CreateEpisodeRequest.newBuilder()
            .setTitle(title)
            .build()
        return client.createEpisode(request).await().indefinitely().episode
    }

    @Transactional
    fun createTestGuest(): Guest {
        val guest = Guest().apply {
            name = "Test Guest"
            email = "guest@example.com"
        }
        guestRepository.persistAndFlush(guest)
        return guest
    }

    private fun transitionTo(
        episodeId: String,
        status: ProtoEpisodeStatus,
    ): com.akaitigo.podflow.grpc.UpdateEpisodeResponse {
        val updatedProto = ProtoEpisode.newBuilder()
            .setId(episodeId)
            .setStatus(status)
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        return client.updateEpisode(request).await().indefinitely()
    }
}
