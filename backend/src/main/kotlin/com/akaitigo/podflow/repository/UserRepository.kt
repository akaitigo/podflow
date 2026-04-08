package com.akaitigo.podflow.repository

import com.akaitigo.podflow.model.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/** Repository for user account persistence. */
@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, UUID> {

    /** Find a user by username, returning null if not found. */
    fun findByUsername(username: String): User? =
        find("username", username).firstResult()
}
