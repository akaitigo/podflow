package com.akaitigo.podflow.auth

import jakarta.enterprise.context.ApplicationScoped
import org.mindrot.jbcrypt.BCrypt

/** Utility for hashing and verifying passwords using bcrypt. */
@ApplicationScoped
class PasswordHasher {

    /** Hash a plain-text password. */
    fun hash(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))

    /** Verify a plain-text password against a bcrypt hash. */
    fun verify(password: String, hashedPassword: String): Boolean =
        BCrypt.checkpw(password, hashedPassword)

    companion object {
        private const val BCRYPT_ROUNDS = 12
    }
}
