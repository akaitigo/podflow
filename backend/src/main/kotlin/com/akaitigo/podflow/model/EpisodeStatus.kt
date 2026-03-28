package com.akaitigo.podflow.model

/** Workflow stages of a podcast episode, matching the kanban columns. */
enum class EpisodeStatus {
    PLANNING,
    GUEST_COORDINATION,
    RECORDING,
    EDITING,
    REVIEW,
    PUBLISHED,
    ;

    /** Returns the set of statuses this status is allowed to transition to. */
    fun allowedTransitions(): Set<EpisodeStatus> =
        when (this) {
            PLANNING -> setOf(GUEST_COORDINATION, RECORDING)
            GUEST_COORDINATION -> setOf(RECORDING)
            RECORDING -> setOf(EDITING)
            EDITING -> setOf(REVIEW)
            REVIEW -> setOf(EDITING, PUBLISHED)
            PUBLISHED -> emptySet()
        }

    /** Returns true if transitioning from this status to [target] is valid. */
    fun canTransitionTo(target: EpisodeStatus): Boolean = target in allowedTransitions()
}
