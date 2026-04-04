package com.akaitigo.podflow.service

import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Validates podcast audio URLs.
 *
 * Extracted from [EpisodeGrpcService] to keep function count within the detekt threshold.
 * Owns the DNS executor lifecycle: the executor is created at injection time and
 * shut down gracefully by [@PreDestroy].
 */
@ApplicationScoped
class AudioUrlValidator {

    /**
     * Single-threaded executor for DNS resolution with timeout.
     * isDaemon = true prevents the thread from blocking JVM shutdown if @PreDestroy is not called.
     */
    private val dnsExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dns-resolver").apply { isDaemon = true }
    }

    @PreDestroy
    fun shutdown() {
        dnsExecutor.shutdown()
    }

    /**
     * Validates that [url] is a well-formed, publicly routable https URL.
     *
     * @throws StatusRuntimeException with INVALID_ARGUMENT if the URL is malformed, non-https,
     *   or resolves to a private/loopback address.
     * @throws StatusRuntimeException with DEADLINE_EXCEEDED if DNS resolution times out.
     */
    fun validate(url: String) {
        validateLength(url)
        val parsed = parseUri(url)
        checkScheme(parsed.scheme)
        checkHost(parsed.host)
        rejectPrivateHost(parsed.host)
    }

    private fun validateLength(url: String) {
        if (url.length > MAX_AUDIO_URL_LENGTH) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription(
                    "audio_url must not exceed $MAX_AUDIO_URL_LENGTH characters",
                ),
            )
        }
    }

    private fun parseUri(url: String): java.net.URI =
        try {
            java.net.URI(url)
        } catch (_: java.net.URISyntaxException) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("audio_url must be a valid URL"),
            )
        }

    private fun checkScheme(scheme: String?) {
        if (scheme != "https") {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("audio_url must use https scheme"),
            )
        }
    }

    private fun checkHost(host: String?) {
        if (host.isNullOrBlank()) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("audio_url must have a valid host"),
            )
        }
    }

    private fun rejectPrivateHost(host: String) {
        val resolved = resolveHostOrThrow(host)
        if (isNonRoutableAddress(resolved)) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription(
                    "audio_url must not point to a private/local address",
                ),
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

    /** Resolve a hostname with a timeout to prevent blocking threads indefinitely. */
    private fun resolveDnsWithTimeout(host: String, timeoutSeconds: Long): java.net.InetAddress {
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

    companion object {
        const val MAX_AUDIO_URL_LENGTH = 2048

        /** DNS resolution timeout in seconds to prevent thread starvation from slow/malicious DNS. */
        private const val DNS_TIMEOUT_SECONDS = 5L
    }
}
