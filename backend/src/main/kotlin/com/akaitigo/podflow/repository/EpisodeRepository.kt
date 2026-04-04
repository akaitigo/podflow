package com.akaitigo.podflow.repository

import com.akaitigo.podflow.model.Episode
import com.akaitigo.podflow.model.EpisodeStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/** Repository for [Episode] persistence operations. */
@ApplicationScoped
class EpisodeRepository : PanacheRepositoryBase<Episode, UUID> {

    /**
     * Returns a page of episodes ordered by creation date (newest first).
     * Uses LEFT JOIN FETCH to eagerly load the guest association and avoid N+1 queries.
     */
    fun listWithGuest(offset: Int, pageSize: Int): List<Episode> =
        getEntityManager()
            .createQuery(
                "SELECT e FROM Episode e LEFT JOIN FETCH e.guest ORDER BY e.createdAt DESC",
                Episode::class.java,
            )
            .setFirstResult(offset * pageSize)
            .setMaxResults(pageSize)
            .resultList

    /**
     * Returns a page of episodes filtered by [status], ordered by creation date (newest first).
     * Uses LEFT JOIN FETCH to eagerly load the guest association and avoid N+1 queries.
     */
    fun listByStatusWithGuest(status: EpisodeStatus, offset: Int, pageSize: Int): List<Episode> =
        getEntityManager()
            .createQuery(
                "SELECT e FROM Episode e LEFT JOIN FETCH e.guest WHERE e.status = :status ORDER BY e.createdAt DESC",
                Episode::class.java,
            )
            .setParameter("status", status)
            .setFirstResult(offset * pageSize)
            .setMaxResults(pageSize)
            .resultList
}
