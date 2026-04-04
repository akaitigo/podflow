package com.akaitigo.podflow.service

import com.akaitigo.podflow.grpc.CreateEpisodeRequest
import com.akaitigo.podflow.grpc.DeleteEpisodeRequest
import com.akaitigo.podflow.grpc.EpisodeService
import com.akaitigo.podflow.grpc.GetEpisodeRequest
import com.akaitigo.podflow.grpc.ListEpisodesRequest
import com.akaitigo.podflow.grpc.UpdateEpisodeRequest
import com.google.protobuf.FieldMask
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
    fun `createEpisode with guest populates guest_name`() {
        val guest = createTestGuest()

        val request = CreateEpisodeRequest.newBuilder()
            .setTitle("Guest Name Episode")
            .setGuestId(requireNotNull(guest.id).toString())
            .build()

        val response = client.createEpisode(request).await().indefinitely()
        assertEquals("Test Guest", response.episode.guestName)
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
    fun `listEpisodes caps page size at 100`() {
        createTestEpisode("Capped Episode")

        val request = ListEpisodesRequest.newBuilder()
            .setPageSize(200)
            .build()

        val response = client.listEpisodes(request).await().indefinitely()
        // Should not crash; result count is capped at MAX_PAGE_SIZE
        assertTrue(response.episodesList.size <= 100)
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
    fun `updateEpisode with audio_url exceeding 2048 chars fails`() {
        val created = createTestEpisode("Audio URL Length Test")
        val longUrl = "https://" + "a".repeat(2041) + ".com/f"  // 2050 chars total

        val updatedProto = com.akaitigo.podflow.grpc.Episode.newBuilder()
            .setId(created.id)
            .setAudioUrl(longUrl)
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("2048"))
    }

    @Test
    fun `updateEpisode with http audio_url fails`() {
        val created = createTestEpisode("HTTP Audio URL Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setAudioUrl("http://insecure.example.com/audio.mp3")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("https"))
    }

    @Test
    fun `updateEpisode with valid https audio_url succeeds`() {
        val created = createTestEpisode("HTTPS Audio URL Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setAudioUrl("https://storage.googleapis.com/audio.mp3")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val response = client.updateEpisode(request).await().indefinitely()
        assertEquals("https://storage.googleapis.com/audio.mp3", response.episode.audioUrl)
    }

    @Test
    fun `updateEpisode with localhost audio_url fails`() {
        val created = createTestEpisode("SSRF Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setAudioUrl("https://localhost/audio.mp3")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("private"))
    }

    @Test
    fun `updateEpisode with private IP audio_url fails`() {
        val created = createTestEpisode("SSRF Private IP Test")

        val updatedProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setAudioUrl("https://127.0.0.1/audio.mp3")
            .build()

        val request = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updatedProto)
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.updateEpisode(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("private"))
    }

    @Test
    fun `updateEpisode without mask preserves optional fields when omitted`() {
        val created = createTestEpisode("Preserve Fields Test")

        // First set description
        val withDesc = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setDescription("Some description")
            .build()
        val setRequest = UpdateEpisodeRequest.newBuilder()
            .setEpisode(withDesc)
            .build()
        val setResponse = client.updateEpisode(setRequest).await().indefinitely()
        assertEquals("Some description", setResponse.episode.description)

        // Update without mask and empty description -> should preserve existing value
        val updateProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setTitle("Preserve Fields Test")
            .build()
        val updateRequest = UpdateEpisodeRequest.newBuilder()
            .setEpisode(updateProto)
            .build()
        val updateResponse = client.updateEpisode(updateRequest).await().indefinitely()
        assertEquals("Some description", updateResponse.episode.description)
    }

    @Test
    fun `updateEpisode with mask clears optional fields with empty string`() {
        val created = createTestEpisode("Clear Fields Test")

        // First set description
        val withDesc = ProtoEpisode.newBuilder()
            .setId(created.id)
            .setDescription("Some description")
            .build()
        val setRequest = UpdateEpisodeRequest.newBuilder()
            .setEpisode(withDesc)
            .build()
        val setResponse = client.updateEpisode(setRequest).await().indefinitely()
        assertEquals("Some description", setResponse.episode.description)

        // Clear description via update_mask
        val clearProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .build()
        val clearRequest = UpdateEpisodeRequest.newBuilder()
            .setEpisode(clearProto)
            .setUpdateMask(FieldMask.newBuilder().addPaths("description").build())
            .build()
        val clearResponse = client.updateEpisode(clearRequest).await().indefinitely()
        assertEquals("", clearResponse.episode.description)
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
    fun `updateEpisode with mask clears guest when guest_id is empty`() {
        val guest = createTestGuest()

        val createRequest = CreateEpisodeRequest.newBuilder()
            .setTitle("Guest Clear Test")
            .setGuestId(requireNotNull(guest.id).toString())
            .build()
        val created = client.createEpisode(createRequest).await().indefinitely().episode
        assertEquals(requireNotNull(guest.id).toString(), created.guestId)

        val clearProto = ProtoEpisode.newBuilder()
            .setId(created.id)
            .build()
        val clearRequest = UpdateEpisodeRequest.newBuilder()
            .setEpisode(clearProto)
            .setUpdateMask(FieldMask.newBuilder().addPaths("guest_id").build())
            .build()
        val cleared = client.updateEpisode(clearRequest).await().indefinitely().episode
        assertEquals("", cleared.guestId)
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

    @Test
    fun `getEpisode with guest does not throw LazyInitializationException`() {
        val guest = createTestGuest()
        val createReq = CreateEpisodeRequest.newBuilder()
            .setTitle("Guest Lazy Load Test")
            .setGuestId(requireNotNull(guest.id).toString())
            .build()
        val created = client.createEpisode(createReq).await().indefinitely().episode

        val getReq = GetEpisodeRequest.newBuilder()
            .setId(created.id)
            .build()
        val response = client.getEpisode(getReq).await().indefinitely()
        assertEquals("Test Guest", response.episode.guestName)
    }

    @Test
    fun `listEpisodes with guest does not throw LazyInitializationException`() {
        val guest = createTestGuest()
        val createReq = CreateEpisodeRequest.newBuilder()
            .setTitle("List Guest Lazy Test")
            .setGuestId(requireNotNull(guest.id).toString())
            .build()
        client.createEpisode(createReq).await().indefinitely()

        val listReq = ListEpisodesRequest.getDefaultInstance()
        val response = client.listEpisodes(listReq).await().indefinitely()
        val ep = response.episodesList.first { it.title == "List Guest Lazy Test" }
        assertEquals("Test Guest", ep.guestName)
    }

    @Test
    fun `listEpisodes with oversized page_token fails`() {
        val request = ListEpisodesRequest.newBuilder()
            .setPageToken("10001")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            client.listEpisodes(request).await().indefinitely()
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
        assertTrue(requireNotNull(exception.status.description).contains("10000"))
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
