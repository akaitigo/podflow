package com.akaitigo.podflow

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthResourceTest {

    @Test
    fun `health endpoint returns ok`() {
        given()
            .`when`().get("/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("ok"))
    }
}
