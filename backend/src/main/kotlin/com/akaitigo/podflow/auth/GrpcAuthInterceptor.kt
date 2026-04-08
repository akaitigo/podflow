package com.akaitigo.podflow.auth

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.auth.principal.ParseException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.microprofile.jwt.JsonWebToken

/**
 * gRPC server interceptor that validates JWT tokens from the Authorization metadata header.
 *
 * Extracts the Bearer token, parses and validates it, and stores the resulting
 * [JsonWebToken] in the gRPC [Context] for downstream service access.
 */
@Singleton
@GlobalInterceptor
class GrpcAuthInterceptor @Inject constructor(
    private val jwtParser: JWTParser,
) : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val jwt = extractAndValidateToken(call, headers)
            ?: return NoOpListener()

        val ctx = Context.current().withValue(JWT_CONTEXT_KEY, jwt)
        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun <ReqT, RespT> extractAndValidateToken(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
    ): JsonWebToken? {
        val authHeader = headers.get(AUTHORIZATION_KEY)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            call.close(
                Status.UNAUTHENTICATED.withDescription(
                    "Missing or invalid Authorization header",
                ),
                Metadata(),
            )
            return null
        }

        val token = authHeader.substring(BEARER_PREFIX.length)

        return try {
            jwtParser.parse(token)
        } catch (e: ParseException) {
            call.close(
                Status.UNAUTHENTICATED.withDescription(
                    "Invalid or expired token: ${e.message}",
                ),
                Metadata(),
            )
            null
        }
    }

    /** A no-op listener returned when the call is rejected. */
    private class NoOpListener<ReqT> : ServerCall.Listener<ReqT>()

    companion object {
        /** Metadata key for the Authorization header. */
        val AUTHORIZATION_KEY: Metadata.Key<String> =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)

        /** gRPC Context key for the parsed JWT. */
        val JWT_CONTEXT_KEY: Context.Key<JsonWebToken> =
            Context.key("jwt")

        private const val BEARER_PREFIX = "Bearer "
    }
}
