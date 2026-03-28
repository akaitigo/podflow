package com.akaitigo.podflow.repository

import com.akaitigo.podflow.model.Episode
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/** Repository for [Episode] persistence operations. */
@ApplicationScoped
class EpisodeRepository : PanacheRepositoryBase<Episode, UUID>
