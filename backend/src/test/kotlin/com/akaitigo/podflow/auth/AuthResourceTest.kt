package com.akaitigo.podflow.auth

import com.akaitigo.podflow.model.User
import com.akaitigo.podflow.repository.UserRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
class AuthResourceTest {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var passwordHasher: PasswordHasher

    @BeforeEach
    @Transactional
    fun cleanup() {
        userRepository.deleteAll()
    }

    @Test
    fun `register creates user and returns token`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"newuser","password":"password123","displayName":"New User"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(201)
            .body("token", notNullValue())
            .body("username", equalTo("newuser"))
            .body("displayName", equalTo("New User"))
    }

    @Test
    fun `register with blank username fails`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"","password":"password123","displayName":"Test"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(400)
            .body("error", equalTo("Username is required"))
    }

    @Test
    fun `register with short password fails`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"testuser","password":"short","displayName":"Test"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(400)
            .body("error", equalTo("Password must be at least 8 characters"))
    }

    @Test
    fun `register with invalid username characters fails`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"user@name","password":"password123","displayName":"Test"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(400)
            .body("error", equalTo("Username must contain only alphanumeric characters, hyphens, and underscores"))
    }

    @Test
    fun `register with duplicate username fails`() {
        createTestUser("existing", "password123")

        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"existing","password":"password123","displayName":"Test"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(409)
            .body("error", equalTo("Username already exists"))
    }

    @Test
    fun `login with valid credentials returns token`() {
        createTestUser("loginuser", "correctpassword")

        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"loginuser","password":"correctpassword"}""")
            .`when`()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo("loginuser"))
    }

    @Test
    fun `login with wrong password fails`() {
        createTestUser("loginuser2", "correctpassword")

        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"loginuser2","password":"wrongpassword"}""")
            .`when`()
            .post("/auth/login")
            .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username or password"))
    }

    @Test
    fun `login with nonexistent user fails`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"ghost","password":"password123"}""")
            .`when`()
            .post("/auth/login")
            .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username or password"))
    }

    @Test
    fun `login with blank fields fails`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"","password":""}""")
            .`when`()
            .post("/auth/login")
            .then()
            .statusCode(400)
            .body("error", equalTo("Username and password are required"))
    }

    @Test
    fun `register with username exceeding 100 chars fails`() {
        val longUsername = "a".repeat(101)

        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"$longUsername","password":"password123","displayName":"Test"}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(400)
            .body("error", equalTo("Username must not exceed 100 characters"))
    }

    @Test
    fun `register without displayName uses username`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"autoname","password":"password123","displayName":""}""")
            .`when`()
            .post("/auth/register")
            .then()
            .statusCode(201)
            .body("displayName", equalTo("autoname"))
    }

    @Test
    fun `health endpoint is accessible without auth`() {
        given()
            .`when`()
            .get("/health")
            .then()
            .statusCode(200)
    }

    @Transactional
    fun createTestUser(username: String, password: String): User {
        val user = User().apply {
            this.username = username
            this.passwordHash = passwordHasher.hash(password)
            this.displayName = username
            this.role = "USER"
            this.createdAt = Instant.now()
            this.updatedAt = Instant.now()
        }
        userRepository.persistAndFlush(user)
        return user
    }
}
