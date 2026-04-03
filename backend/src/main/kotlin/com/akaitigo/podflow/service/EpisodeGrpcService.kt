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
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.akaitigo.podflow.grpc.EpisodeStatus as ProtoEpisodeStatus

/** gRPC service implementing Episode CRUD operations. */
@GrpcService
@Blocking
class EpisodeGrpcService @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val guestRepository: GuestRepository,
    private val episodeMapper: EpisodeMapper,
) : EpisodeService {

    @Transactional
    override fun createEpisode(request: CreateEpisodeRequest): Uni<CreateEpisodeResponse> =
        Uni.createFrom().item {
            validateTitle(request.title)
            validateFieldLength(request.description, "description", MAX_DESCRIPTION_LENGTH)

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
            val pageSize = when {
                request.pageSize <= 0 -> DEFAULT_PAGE_SIZE
                request.pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
                else -> request.pageSize
            }
            val offset = parsePageToken(request.pageToken)

            val hasStatusFilter =
                request.statusFilter != ProtoEpisodeStatus.EPISODE_STATUS_UNSPECIFIED &&
                    request.statusFilter != ProtoEpisodeStatus.UNRECOGNIZED

            val query = if (hasStatusFilter) {
                val modelStatus = episodeMapper.toModelStatus(request.statusFilter)
                episodeRepository.find("status = ?1 order by createdAt desc", modelStatus)
            } else {
                episodeRepository.find("order by createdAt desc")
            }

            val episodes = query.page(offset, pageSize).list()

            val builder = ListEpisodesResponse.newBuilder()
            episodes.forEach { builder.addEpisodes(episodeMapper.toProto(it)) }

            if (episodes.size == pageSize) {
                builder.nextPageToken = (offset + 1).toString()
            }

            builder.build()
        }

    @Transactional
    override fun updateEpisode(request: UpdateEpisodeRequest): Uni<UpdateEpisodeResponse> =
        Uni.createFrom().item {
            val protoEpisode = request.episode
                ?: throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Episode field is required"),
                )

            val id = parseUuid(protoEpisode.id, "id")
            val existing = findEpisodeOrThrow(id)

            val mask = request.updateMask
            if (mask != null && mask.pathsCount > 0) {
                applyMaskedUpdates(existing, protoEpisode, mask)
            } else {
                applyFieldUpdates(existing, protoEpisode)
                applyStatusUpdate(existing, protoEpisode)
                applyGuestUpdate(existing, protoEpisode)
            }

            existing.updatedAt = Instant.now()
            episodeRepository.persistAndFlush(existing)

            UpdateEpisodeResponse.newBuilder()
                .setEpisode(episodeMapper.toProto(existing))
                .build()
        }

    @Transactional
    override fun deleteEpisode(request: DeleteEpisodeRequest): Uni<DeleteEpisodeResponse> =
        Uni.createFrom().item {
            val id = parseUuid(request.id, "id")
            val episode = findEpisodeOrThrow(id)

            if (episode.status == EpisodeStatus.PUBLISHED) {
                throw StatusRuntimeException(
                    Status.FAILED_PRECONDITION.withDescription(
                        "Cannot delete a published episode: $id",
                    ),
                )
            }

            episodeRepository.delete(episode)

            DeleteEpisodeResponse.getDefaultInstance()
        }

    private fun applyMaskedUpdates(
        existing: Episode,
        proto: com.akaitigo.podflow.grpc.Episode,
        mask: com.google.protobuf.FieldMask,
    ) {
        for (path in mask.pathsList) {
            when (path) {
                "title" -> {
                    validateTitle(proto.title)
                    existing.title = proto.title
                }
                "description" -> {
                    validateFieldLength(proto.description, "description", MAX_DESCRIPTION_LENGTH)
                    existing.description = proto.description.ifEmpty { null }
                }
                "audio_url" -> {
                    if (proto.audioUrl.isNotEmpty()) {
                        validateAudioUrl(proto.audioUrl)
                    }
                    existing.audioUrl = proto.audioUrl.ifEmpty { null }
                }
                "show_notes" -> {
                    validateFieldLength(proto.showNotes, "show_notes", MAX_SHOW_NOTES_LENGTH)
                    existing.showNotes = proto.showNotes.ifEmpty { null }
                }
                "status" -> applyStatusUpdate(existing, proto)
                "guest_id" -> applyGuestUpdate(existing, proto, clearOnEmpty = true)
                else -> throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "Unknown update_mask path: $path",
                    ),
                )
            }
        }
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
            validateFieldLength(proto.description, "description", MAX_DESCRIPTION_LENGTH)
            existing.description = proto.description
        }
        if (proto.audioUrl.isNotEmpty()) {
            validateAudioUrl(proto.audioUrl)
            existing.audioUrl = proto.audioUrl
        }
        if (proto.showNotes.isNotEmpty()) {
            validateFieldLength(proto.showNotes, "show_notes", MAX_SHOW_NOTES_LENGTH)
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
        clearOnEmpty: Boolean = false,
    ) {
        if (proto.guestId.isNotEmpty()) {
            val guestId = parseUuid(proto.guestId, "guest_id")
            val guest = guestRepository.findById(guestId)
                ?: throw StatusRuntimeException(
                    Status.NOT_FOUND.withDescription("Guest not found: ${proto.guestId}"),
                )
            existing.guest = guest
        } else if (clearOnEmpty) {
            existing.guest = null
        }
    }

    private fun findEpisodeOrThrow(id: UUID): Episode =
        episodeRepository.findById(id)
            ?: throw StatusRuntimeException(
                Status.NOT_FOUND.withDescription("Episode not found: $id"),
            )

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private const val MAX_TITLE_LENGTH = 500
        private const val MAX_DESCRIPTION_LENGTH = 50_000
        private const val MAX_SHOW_NOTES_LENGTH = 50_000

        fun validateTitle(title: String) {
            if (title.isBlank()) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Title must not be blank"),
                )
            }
            if (title.length > MAX_TITLE_LENGTH) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "Title must not exceed $MAX_TITLE_LENGTH characters",
                    ),
                )
            }
        }

        fun validateFieldLength(value: String, fieldName: String, maxLength: Int) {
            if (value.length > maxLength) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "$fieldName must not exceed $maxLength characters",
                    ),
                )
            }
        }

        fun validateAudioUrl(url: String) {
            val parsed = try {
                java.net.URI(url)
            } catch (_: java.net.URISyntaxException) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "audio_url must be a valid URL",
                    ),
                )
            }
            val reason = when {
                parsed.scheme != "https" -> "audio_url must use https scheme"
                parsed.host.isNullOrBlank() -> "audio_url must have a valid host"
                else -> null
            }
            if (reason != null) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(reason),
                )
            }
            rejectPrivateHost(parsed.host)
        }

        /** DNS resolution timeout in seconds to prevent thread starvation from slow/malicious DNS. */
        private const val DNS_TIMEOUT_SECONDS = 5L

        private fun rejectPrivateHost(host: String) {
            val resolved = resolveHostOrThrow(host)
            if (isNonRoutableAddress(resolved)) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("audio_url must not point to a private/local address"),
                )
            }
        }

        private fun resolveHostOrThrow(host: String): java.net.InetAddress =
            try {
                resolveDnsWithTimeout(host, DNS_TIMEOUT_SECONDS)
            } catch (_: java.net.UnknownHostException) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("audio_url host cannot be resolved: $host"),
                )
            } catch (_: TimeoutException) {
                throw StatusRuntimeException(
                    Status.DEADLINE_EXCEEDED.withDescription(
                        "audio_url host DNS resolution timed out: $host",
                    ),
                )
            }

        private val dnsExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "dns-resolver").apply { isDaemon = true }
        }

        /**
         * Resolve a hostname with a timeout to prevent blocking threads
         * indefinitely on slow or unresponsive DNS servers.
         */
        private fun resolveDnsWithTimeout(
            host: String,
            timeoutSeconds: Long,
        ): java.net.InetAddress {
            val future = dnsExecutor.submit(
                Callable { java.net.InetAddress.getByName(host) },
            )
            return future.get(timeoutSeconds, TimeUnit.SECONDS)
        }

        private fun isNonRoutableAddress(address: java.net.InetAddress): Boolean =
            address.isLoopbackAddress ||
                address.isLinkLocalAddress ||
                address.isSiteLocalAddress ||
                address.isAnyLocalAddress

        fun validateStatusTransition(current: EpisodeStatus, target: EpisodeStatus) {
            if (!current.canTransitionTo(target)) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "Invalid status transition from $current to $target",
                    ),
                )
            }
        }

        fun parseUuid(value: String, fieldName: String): UUID =
            try {
                UUID.fromString(value)
            } catch (_: IllegalArgumentException) {
                throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Invalid UUID for $fieldName: $value"),
                )
            }

        fun parsePageToken(token: String): Int {
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
    }
}
