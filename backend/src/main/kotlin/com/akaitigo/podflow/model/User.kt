package com.akaitigo.podflow.model

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** A podflow user account. */
@Entity
@Table(name = "users")
class User : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "username", nullable = false, unique = true, length = 100)
    var username: String = ""

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""

    @Column(name = "display_name", nullable = false)
    var displayName: String = ""

    @Column(name = "role", nullable = false, length = 50)
    var role: String = "USER"

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
