package com.akaitigo.podflow.repository

import com.akaitigo.podflow.model.Guest
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/** Repository for [Guest] persistence operations. */
@ApplicationScoped
class GuestRepository : PanacheRepositoryBase<Guest, UUID>
