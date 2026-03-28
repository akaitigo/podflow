/** Workflow stages of a podcast episode, matching kanban columns. */
export const EpisodeStatus = {
	PLANNING: "PLANNING",
	GUEST_COORDINATION: "GUEST_COORDINATION",
	RECORDING: "RECORDING",
	EDITING: "EDITING",
	REVIEW: "REVIEW",
	PUBLISHED: "PUBLISHED",
} as const;

export type EpisodeStatus = (typeof EpisodeStatus)[keyof typeof EpisodeStatus];

/** All statuses in kanban column order. */
export const EPISODE_STATUSES: readonly EpisodeStatus[] = [
	EpisodeStatus.PLANNING,
	EpisodeStatus.GUEST_COORDINATION,
	EpisodeStatus.RECORDING,
	EpisodeStatus.EDITING,
	EpisodeStatus.REVIEW,
	EpisodeStatus.PUBLISHED,
];

/** Human-readable labels for each status. */
export const STATUS_LABELS: Record<EpisodeStatus, string> = {
	PLANNING: "Planning",
	GUEST_COORDINATION: "Guest Coordination",
	RECORDING: "Recording",
	EDITING: "Editing",
	REVIEW: "Review",
	PUBLISHED: "Published",
};

/** Valid status transitions matching backend rules. */
const ALLOWED_TRANSITIONS: Record<EpisodeStatus, readonly EpisodeStatus[]> = {
	PLANNING: [EpisodeStatus.GUEST_COORDINATION, EpisodeStatus.RECORDING],
	GUEST_COORDINATION: [EpisodeStatus.RECORDING],
	RECORDING: [EpisodeStatus.EDITING],
	EDITING: [EpisodeStatus.REVIEW],
	REVIEW: [EpisodeStatus.EDITING, EpisodeStatus.PUBLISHED],
	PUBLISHED: [],
};

/** Check whether a transition from one status to another is valid. */
export function canTransition(from: EpisodeStatus, to: EpisodeStatus): boolean {
	return ALLOWED_TRANSITIONS[from].includes(to);
}

/** A podcast episode. */
export interface Episode {
	readonly id: string;
	readonly title: string;
	readonly description: string;
	readonly status: EpisodeStatus;
	readonly guestId: string;
	readonly guestName: string;
	readonly audioUrl: string;
	readonly showNotes: string;
	readonly publishedAt: string | null;
	readonly createdAt: string;
	readonly updatedAt: string;
}

/** Payload for creating a new episode. */
export interface CreateEpisodeInput {
	readonly title: string;
	readonly description: string;
	readonly guestId: string;
}

/** Payload for updating an existing episode. */
export interface UpdateEpisodeInput {
	readonly id: string;
	readonly title?: string;
	readonly description?: string;
	readonly status?: EpisodeStatus;
	readonly showNotes?: string;
}
