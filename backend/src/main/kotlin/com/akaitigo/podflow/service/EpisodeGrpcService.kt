package com.akaitigo.podflow.service

import com.akaitigo.podflow.grpc.CreateEpisodeRequest
import com.akaitigo.podflow.grpc.CreateEpisodeResponse
import com.akaitigo.podflow.grpc.DeleteEpisodeRequest
import com.akaitigo.podflow.grpc.DeleteEpisodeResponse
import com.akaitigo.podflow.grpc.EpisodeService
import com.akaitigo.podflow.grpc.GetEpisodeRequest
import com.akaitigo.podflow.grpc.GetEpisodeResponse
import com.akaitigo.podflow.grpc.ListEpisodesRequest
import com.akaitigo.podflow.grpc.ListEpisodesResponse
import com.akaitigo.podflow.grpc.UpdateEpisodeRequest
import com.akaitigo.podflow.grpc.UpdateEpisodeResponse
import com.akaitigo.podflow.model.Episode
import com.akaitigo.podflow.model.EpisodeStatus
import com.akaitigo.podflow.repository.EpisodeRepository
import com.akaitigo.podflow.repository.GuestRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID
import com.akaitigo.podflow.grpc.EpisodeStatus as ProtoEpisodeStatus

/** gRPC service implementing Episode CRUD operations. */
@GrpcService
@Blocking
@Transactional
class EpisodeGrpcService : EpisodeService {

    @Inject
    lateinit var episodeRepository: EpisodeRepository

    @Inject
    lateinit var guestRepository: GuestRepository

    @Inject
    lateinit var episodeMapper: EpisodeMapper

    override fun createEpisode(request: CreateEpisodeRequest): Uni<CreateEpisodeResponse> =
        Uni.createFrom().item {
            validateTitle(request.title)

            val episode = Episode().apply {
                title = request.title
                description = request.description.ifEmpty { null }
                status = EpisodeStatus.PLANNING
                createdAt = Instant.now()
                updatedAt = Instant.now()
            }

            if (request.guestId.isNotEmpty()) {
                val guestId = parseUuid(request.guestId, "guest_id")
                val guest = guestRepository.findById(guestId)
                    ?: throw StatusRuntimeException(
                        Status.NOT_FOUND.withDescription("Guest not found: ${request.guestId}"),
                    )
                episode.guest = guest
            }

            episodeRepository.persistAndFlush(episode)

            CreateEpisodeResponse.newBuilder()
                .setEpisode(episodeMapper.toProto(episode))
                .build()
        }

    override fun getEpisode(request: GetEpisodeRequest): Uni<GetEpisodeResponse> =
        Uni.createFrom().item {
            val id = parseUuid(request.id, "id")
            val episode = findEpisodeOrThrow(id)

            GetEpisodeResponse.newBuilder()
                .setEpisode(episodeMapper.toProto(episode))
                .build()
        }

    override fun listEpisodes(request: ListEpisodesRequest): Uni<ListEpisodesResponse> =
        Uni.createFrom().item {
            val pageSize = if (request.pageSize > 0) { request.pageSize } else { DEFAULT_PAGE_SIZE }
            val offset = parsePageToken(request.pageToken)

            val hasStatusFilter =
                request.statusFilter != ProtoEpisodeStatus.EPISODE_STATUS_UNSPECIFIED &&
                    request.statusFilter != ProtoEpisodeStatus.UNRECOGNIZED

            val query = if (hasStatusFilter) {
                val modelStatus = episodeMapper.toModelStatus(request.statusFilter)
                episodeRepository.find("status", modelStatus)
            } else {
                episodeRepository.findAll()
            }

            val episodes = query.page(offset, pageSize).list()

            val builder = ListEpisodesResponse.newBuilder()
            episodes.forEach { builder.addEpisodes(episodeMapper.toProto(it)) }

            if (episodes.size == pageSize) {
                builder.nextPageToken = (offset + 1).toString()
            }

            builder.build()
        }

    override fun updateEpisode(request: UpdateEpisodeRequest): Uni<UpdateEpisodeResponse> =
        Uni.createFrom().item {
            val protoEpisode = request.episode
                ?: throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Episode field is required"),
                )

            val id = parseUuid(protoEpisode.id, "id")
            val existing = findEpisodeOrThrow(id)

            applyFieldUpdates(existing, protoEpisode)
            applyStatusUpdate(existing, protoEpisode)
            applyGuestUpdate(existing, protoEpisode)

            existing.updatedAt = Instant.now()
            episodeRepository.persistAndFlush(existing)

            UpdateEpisodeResponse.newBuilder()
                .setEpisode(episodeMapper.toProto(existing))
                .build()
        }

    override fun deleteEpisode(request: DeleteEpisodeRequest): Uni<DeleteEpisodeResponse> =
        Uni.createFrom().item {
            val id = parseUuid(request.id, "id")
            val episode = findEpisodeOrThrow(id)
            episodeRepository.delete(episode)

            DeleteEpisodeResponse.getDefaultInstance()
        }

    private fun applyFieldUpdates(
        existing: Episode,
        proto: com.akaitigo.podflow.grpc.Episode,
    ) {
        if (proto.title.isNotEmpty()) {
            validateTitle(proto.title)
            existing.title = proto.title
        }
        if (proto.description.isNotEmpty()) {
            existing.description = proto.description
        }
        if (proto.audioUrl.isNotEmpty()) {
            existing.audioUrl = proto.audioUrl
        }
        if (proto.showNotes.isNotEmpty()) {
            existing.showNotes = proto.showNotes
        }
    }

    private fun applyStatusUpdate(
        existing: Episode,
        proto: com.akaitigo.podflow.grpc.Episode,
    ) {
        val hasStatusUpdate =
            proto.status != ProtoEpisodeStatus.EPISODE_STATUS_UNSPECIFIED &&
                proto.status != ProtoEpisodeStatus.UNRECOGNIZED

        if (hasStatusUpdate) {
            val newStatus = episodeMapper.toModelStatus(proto.status)
            validateStatusTransition(existing.status, newStatus)
            existing.status = newStatus

            if (newStatus == EpisodeStatus.PUBLISHED) {
                existing.publishedAt = Instant.now()
            }
        }
    }

    private fun applyGuestUpdate(
        existing: Episode,
        proto: com.akaitigo.podflow.grpc.Episode,
    ) {
        if (proto.guestId.isNotEmpty()) {
            val guestId = parseUuid(proto.guestId, "guest_id")
            val guest = guestRepository.findById(guestId)
                ?: throw StatusRuntimeException(
                    Status.NOT_FOUND.withDescription("Guest not found: ${proto.guestId}"),
                )
            existing.guest = guest
        }
    }

    private fun findEpisodeOrThrow(id: UUID): Episode =
        episodeRepository.findById(id)
            ?: throw StatusRuntimeException(
                Status.NOT_FOUND.withDescription("Episode not found: $id"),
            )

    private fun validateTitle(title: String) {
        if (title.isBlank()) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("Title must not be blank"),
            )
        }
    }

    private fun validateStatusTransition(current: EpisodeStatus, target: EpisodeStatus) {
        if (!current.canTransitionTo(target)) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription(
                    "Invalid status transition from $current to $target",
                ),
            )
        }
    }

    private fun parseUuid(value: String, fieldName: String): UUID =
        try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("Invalid UUID for $fieldName: $value"),
            )
        }

    private fun parsePageToken(token: String): Int {
        if (token.isEmpty()) {
            return 0
        }
        return try {
            val page = token.toInt()
            if (page < 0) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("page_token must be non-negative"),
                )
            }
            page
        } catch (_: NumberFormatException) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("Invalid page_token: $token"),
            )
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}
