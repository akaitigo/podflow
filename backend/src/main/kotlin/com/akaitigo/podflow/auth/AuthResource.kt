package com.akaitigo.podflow.auth

import com.akaitigo.podflow.model.User
import com.akaitigo.podflow.repository.UserRepository
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant

/** REST resource for authentication (login and register). */
@Path("/auth")
class AuthResource @Inject constructor(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
) {

    /** Authenticate a user and return a JWT token. */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun login(request: LoginRequest): Response = try {
        validateLoginRequest(request)
        val user = authenticateUser(request.username, request.password)
        val token = generateTokenForUser(user)
        Response.ok(LoginResponse(token, user.username, user.displayName)).build()
    } catch (e: AuthException) {
        Response.status(e.status).entity(ErrorResponse(e.message)).build()
    }

    /** Register a new user account. */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    fun register(request: RegisterRequest): Response = try {
        validateRegisterRequest(request)
        val user = createUser(request)
        val token = generateTokenForUser(user)
        Response.status(Response.Status.CREATED)
            .entity(LoginResponse(token, user.username, user.displayName))
            .build()
    } catch (e: AuthException) {
        Response.status(e.status).entity(ErrorResponse(e.message)).build()
    }

    private fun validateLoginRequest(request: LoginRequest) {
        if (request.username.isBlank() || request.password.isBlank()) {
            throw AuthException(
                Response.Status.BAD_REQUEST,
                "Username and password are required",
            )
        }
        validatePasswordByteLength(request.password)
    }

    private fun authenticateUser(username: String, password: String): User {
        val user = userRepository.findByUsername(username)
            ?: throw AuthException(
                Response.Status.UNAUTHORIZED,
                "Invalid username or password",
            )

        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw AuthException(
                Response.Status.UNAUTHORIZED,
                "Invalid username or password",
            )
        }

        return user
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        validateUsernameFormat(request.username)
        validateDisplayNameLength(request.displayName)
        validatePasswordStrength(request.password)
        validatePasswordByteLength(request.password)
        ensureUsernameAvailable(request.username)
    }

    private fun validateDisplayNameLength(displayName: String) {
        if (displayName.isNotBlank() && displayName.length > MAX_DISPLAY_NAME_LENGTH) {
            throw AuthException(
                Response.Status.BAD_REQUEST,
                "Display name must not exceed $MAX_DISPLAY_NAME_LENGTH characters",
            )
        }
    }

    private fun validateUsernameFormat(username: String) {
        val error = usernameFormatError(username)
        if (error != null) {
            throw AuthException(Response.Status.BAD_REQUEST, error)
        }
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw AuthException(
                Response.Status.BAD_REQUEST,
                "Password must be at least $MIN_PASSWORD_LENGTH characters",
            )
        }
    }

    private fun validatePasswordByteLength(password: String) {
        if (password.toByteArray(Charsets.UTF_8).size > BCRYPT_MAX_PASSWORD_BYTES) {
            throw AuthException(
                Response.Status.BAD_REQUEST,
                "Password must not exceed $BCRYPT_MAX_PASSWORD_BYTES bytes",
            )
        }
    }

    private fun ensureUsernameAvailable(username: String) {
        if (userRepository.findByUsername(username) != null) {
            throw AuthException(
                Response.Status.CONFLICT,
                "Username already exists",
            )
        }
    }

    private fun createUser(request: RegisterRequest): User {
        val user = User().apply {
            this.username = request.username
            passwordHash = passwordHasher.hash(request.password)
            displayName = request.displayName.ifBlank { request.username }
            role = "USER"
            createdAt = Instant.now()
            updatedAt = Instant.now()
        }
        userRepository.persistAndFlush(user)
        return user
    }

    private fun generateTokenForUser(user: User): String =
        jwtTokenService.generateToken(
            userId = requireNotNull(user.id).toString(),
            username = user.username,
            role = user.role,
        )

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val BCRYPT_MAX_PASSWORD_BYTES = 72
        private const val MIN_USERNAME_LENGTH = 1
        private const val MAX_USERNAME_LENGTH = 50
        private const val MAX_DISPLAY_NAME_LENGTH = 255
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_-]+$")

        /** Returns an error message if the username format is invalid, or null. */
        fun usernameFormatError(username: String): String? = when {
            username.isBlank() -> "Username is required"
            username.length < MIN_USERNAME_LENGTH -> "Username must be at least $MIN_USERNAME_LENGTH characters"
            username.length > MAX_USERNAME_LENGTH ->
                "Username must not exceed $MAX_USERNAME_LENGTH characters"
            !USERNAME_PATTERN.matches(username) ->
                "Username must contain only alphanumeric characters, hyphens, and underscores"
            else -> null
        }
    }
}

/** Internal exception for authentication validation failures. */
private class AuthException(
    val status: Response.Status,
    override val message: String,
) : RuntimeException(message)

/** Login request payload. */
data class LoginRequest(
    val username: String = "",
    val password: String = "",
)

/** Registration request payload. */
data class RegisterRequest(
    val username: String = "",
    val password: String = "",
    val displayName: String = "",
)

/** Successful authentication response. */
data class LoginResponse(
    val token: String,
    val username: String,
    val displayName: String,
)

/** Error response. */
data class ErrorResponse(
    val error: String,
)
