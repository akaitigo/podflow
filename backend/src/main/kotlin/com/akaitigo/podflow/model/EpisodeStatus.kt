package com.akaitigo.podflow.model

/** Workflow stages of a podcast episode, matching the kanban columns. */
enum class EpisodeStatus {
    PLANNING,
    GUEST_COORDINATION,
    RECORDING,
    EDITING,
    REVIEW,
    PUBLISHED,
}
