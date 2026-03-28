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

/** A podcast guest. */
@Entity
@Table(name = "guests")
class Guest : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "email")
    var email: String? = null

    @Column(name = "bio", columnDefinition = "TEXT")
    var bio: String? = null

    /** Social media profile URLs stored as JSON array. */
    @Column(name = "social_links", columnDefinition = "TEXT")
    var socialLinks: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
