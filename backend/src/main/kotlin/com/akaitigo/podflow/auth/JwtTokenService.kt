package com.akaitigo.podflow.auth

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration

/** Service for generating JWT access tokens. */
@ApplicationScoped
class JwtTokenService(
    @param:ConfigProperty(name = "mp.jwt.verify.issuer")
    private val issuer: String,

    @param:ConfigProperty(name = "podflow.jwt.expiration-minutes", defaultValue = "60")
    private val expirationMinutes: Long,
) {

    /** Generate a signed JWT for the given user. */
    fun generateToken(userId: String, username: String, role: String): String =
        Jwt.issuer(issuer)
            .subject(userId)
            .upn(username)
            .groups(setOf(role))
            .claim("displayName", username)
            .expiresIn(Duration.ofMinutes(expirationMinutes))
            .sign()
}
