package com.akaitigo.podflow.service

import com.akaitigo.podflow.model.Episode
import com.akaitigo.podflow.model.EpisodeStatus
import com.akaitigo.podflow.model.Guest
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.util.UUID
import com.akaitigo.podflow.grpc.EpisodeStatus as ProtoEpisodeStatus

class EpisodeMapperTest {

    private val mapper = EpisodeMapper()

    @Test
    fun `toProto maps all fields when fully populated`() {
        val guestId = UUID.randomUUID()
        val episodeId = UUID.randomUUID()
        val now = Instant.parse("2026-01-15T10:30:00Z")
        val publishedAt = Instant.parse("2026-01-20T12:00:00Z")

        val guest = Guest().apply { id = guestId }
        val episode = Episode().apply {
            id = episodeId
            title = "Full Episode"
            description = "A full description"
            status = EpisodeStatus.PUBLISHED
            this.guest = guest
            audioUrl = "https://example.com/audio.mp3"
            showNotes = "# Show Notes"
            this.publishedAt = publishedAt
            createdAt = now
            updatedAt = now
        }

        val proto = mapper.toProto(episode)

        assertEquals(episodeId.toString(), proto.id)
        assertEquals("Full Episode", proto.title)
        assertEquals("A full description", proto.description)
        assertEquals(ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED, proto.status)
        assertEquals(guestId.toString(), proto.guestId)
        assertEquals("https://example.com/audio.mp3", proto.audioUrl)
        assertEquals("# Show Notes", proto.showNotes)
        assertTrue(proto.hasPublishedAt())
        assertEquals(publishedAt.epochSecond, proto.publishedAt.seconds)
        assertEquals(now.epochSecond, proto.createdAt.seconds)
        assertEquals(now.epochSecond, proto.updatedAt.seconds)
    }

    @Test
    fun `toProto maps null optional fields to empty strings`() {
        val episode = Episode().apply {
            title = "Minimal Episode"
            status = EpisodeStatus.PLANNING
            createdAt = Instant.now()
            updatedAt = Instant.now()
        }

        val proto = mapper.toProto(episode)

        assertEquals("Minimal Episode", proto.title)
        assertEquals("", proto.description)
        assertEquals("", proto.audioUrl)
        assertEquals("", proto.showNotes)
        assertEquals("", proto.guestId)
        assertFalse(proto.hasPublishedAt())
    }

    @Test
    fun `toProto handles null id`() {
        val episode = Episode().apply {
            title = "No ID"
            status = EpisodeStatus.PLANNING
            createdAt = Instant.now()
            updatedAt = Instant.now()
        }

        val proto = mapper.toProto(episode)
        assertEquals("", proto.id)
    }

    @ParameterizedTest
    @EnumSource(EpisodeStatus::class)
    fun `toProtoStatus roundtrips all model statuses`(status: EpisodeStatus) {
        val protoStatus = mapper.toProtoStatus(status)
        val roundTripped = mapper.toModelStatus(protoStatus)
        assertEquals(status, roundTripped)
    }

    @Test
    fun `toModelStatus throws for UNSPECIFIED`() {
        val exception = assertThrows<StatusRuntimeException> {
            mapper.toModelStatus(ProtoEpisodeStatus.EPISODE_STATUS_UNSPECIFIED)
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
    }

    @Test
    fun `toModelStatus throws for UNRECOGNIZED`() {
        val exception = assertThrows<StatusRuntimeException> {
            mapper.toModelStatus(ProtoEpisodeStatus.UNRECOGNIZED)
        }
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.code, exception.status.code)
    }

    @Test
    fun `toProto preserves timestamp nanoseconds`() {
        val instant = Instant.ofEpochSecond(1700000000L, 123456789L)
        val episode = Episode().apply {
            title = "Nanos Test"
            status = EpisodeStatus.PLANNING
            createdAt = instant
            updatedAt = instant
        }

        val proto = mapper.toProto(episode)
        assertEquals(1700000000L, proto.createdAt.seconds)
        assertEquals(123456789, proto.createdAt.nanos)
    }

    @Test
    fun `toProto handles empty description as empty string`() {
        val episode = Episode().apply {
            title = "Empty Desc"
            description = ""
            status = EpisodeStatus.PLANNING
            createdAt = Instant.now()
            updatedAt = Instant.now()
        }

        val proto = mapper.toProto(episode)
        assertEquals("", proto.description)
    }

    @Test
    fun `toProto maps max-length title correctly`() {
        val longTitle = "A".repeat(500)
        val episode = Episode().apply {
            title = longTitle
            status = EpisodeStatus.PLANNING
            createdAt = Instant.now()
            updatedAt = Instant.now()
        }

        val proto = mapper.toProto(episode)
        assertEquals(500, proto.title.length)
    }
}
