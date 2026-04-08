package com.akaitigo.podflow.auth

import io.smallrye.jwt.build.Jwt
import java.time.Duration

/** Helper for generating JWT tokens in tests. */
object TestJwtHelper {

    private const val TEST_ISSUER = "https://podflow.akaitigo.com"
    private const val TEST_USER_ID = "00000000-0000-0000-0000-000000000001"
    private const val TEST_USERNAME = "testuser"
    private const val TEST_DISPLAY_NAME = "Test User"
    private const val TEST_ROLE = "USER"

    /** Generate a valid test JWT token. */
    fun generateTestToken(
        userId: String = TEST_USER_ID,
        username: String = TEST_USERNAME,
        displayName: String = TEST_DISPLAY_NAME,
        role: String = TEST_ROLE,
    ): String =
        Jwt.issuer(TEST_ISSUER)
            .subject(userId)
            .upn(username)
            .groups(setOf(role))
            .claim("displayName", displayName)
            .expiresIn(Duration.ofHours(1))
            .sign()

    /** Generate an expired JWT token for testing rejection. */
    fun generateExpiredToken(): String =
        Jwt.issuer(TEST_ISSUER)
            .subject(TEST_USER_ID)
            .upn(TEST_USERNAME)
            .groups(setOf(TEST_ROLE))
            .expiresAt(0)
            .sign()
}
