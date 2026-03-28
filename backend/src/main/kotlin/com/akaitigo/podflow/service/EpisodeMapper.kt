package com.akaitigo.podflow.service

import com.akaitigo.podflow.model.Episode
import com.akaitigo.podflow.model.EpisodeStatus
import com.google.protobuf.Timestamp
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import com.akaitigo.podflow.grpc.Episode as ProtoEpisode
import com.akaitigo.podflow.grpc.EpisodeStatus as ProtoEpisodeStatus

/** Converts between JPA [Episode] entities and proto [ProtoEpisode] messages. */
@ApplicationScoped
class EpisodeMapper {

    /** Converts a JPA entity to a proto message. */
    fun toProto(entity: Episode): ProtoEpisode {
        val builder = ProtoEpisode.newBuilder()
            .setTitle(entity.title)
            .setDescription(entity.description.orEmpty())
            .setStatus(toProtoStatus(entity.status))
            .setAudioUrl(entity.audioUrl.orEmpty())
            .setShowNotes(entity.showNotes.orEmpty())

        entity.id?.let { builder.setId(it.toString()) }
        entity.guest?.id?.let { builder.setGuestId(it.toString()) }
        entity.publishedAt?.let { builder.setPublishedAt(toTimestamp(it)) }
        entity.createdAt.let { builder.setCreatedAt(toTimestamp(it)) }
        entity.updatedAt.let { builder.setUpdatedAt(toTimestamp(it)) }

        return builder.build()
    }

    /** Converts a proto [ProtoEpisodeStatus] to a Kotlin [EpisodeStatus]. */
    fun toModelStatus(proto: ProtoEpisodeStatus): EpisodeStatus =
        when (proto) {
            ProtoEpisodeStatus.EPISODE_STATUS_PLANNING -> EpisodeStatus.PLANNING
            ProtoEpisodeStatus.EPISODE_STATUS_GUEST_COORDINATION -> EpisodeStatus.GUEST_COORDINATION
            ProtoEpisodeStatus.EPISODE_STATUS_RECORDING -> EpisodeStatus.RECORDING
            ProtoEpisodeStatus.EPISODE_STATUS_EDITING -> EpisodeStatus.EDITING
            ProtoEpisodeStatus.EPISODE_STATUS_REVIEW -> EpisodeStatus.REVIEW
            ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED -> EpisodeStatus.PUBLISHED
            ProtoEpisodeStatus.EPISODE_STATUS_UNSPECIFIED, ProtoEpisodeStatus.UNRECOGNIZED ->
                throw io.grpc.StatusRuntimeException(
                    io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid episode status: $proto"),
                )
        }

    /** Converts a Kotlin [EpisodeStatus] to a proto [ProtoEpisodeStatus]. */
    fun toProtoStatus(status: EpisodeStatus): ProtoEpisodeStatus =
        when (status) {
            EpisodeStatus.PLANNING -> ProtoEpisodeStatus.EPISODE_STATUS_PLANNING
            EpisodeStatus.GUEST_COORDINATION -> ProtoEpisodeStatus.EPISODE_STATUS_GUEST_COORDINATION
            EpisodeStatus.RECORDING -> ProtoEpisodeStatus.EPISODE_STATUS_RECORDING
            EpisodeStatus.EDITING -> ProtoEpisodeStatus.EPISODE_STATUS_EDITING
            EpisodeStatus.REVIEW -> ProtoEpisodeStatus.EPISODE_STATUS_REVIEW
            EpisodeStatus.PUBLISHED -> ProtoEpisodeStatus.EPISODE_STATUS_PUBLISHED
        }

    private fun toTimestamp(instant: Instant): Timestamp =
        Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
}
