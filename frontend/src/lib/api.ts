import type { CreateEpisodeInput, Episode, EpisodeStatus, UpdateEpisodeInput } from "../types/episode";

/**
 * API client interface for episode operations.
 * Allows swapping mock and gRPC implementations.
 */
export interface EpisodeApi {
	listEpisodes(): Promise<readonly Episode[]>;
	createEpisode(input: CreateEpisodeInput): Promise<Episode>;
	updateEpisode(input: UpdateEpisodeInput): Promise<Episode>;
	deleteEpisode(id: string): Promise<void>;
}

/** In-memory mock implementation for MVP development. */
export function createMockApi(initialEpisodes: Episode[]): EpisodeApi {
	let episodes = [...initialEpisodes];

	return {
		async listEpisodes(): Promise<readonly Episode[]> {
			return [...episodes];
		},

		async createEpisode(input: CreateEpisodeInput): Promise<Episode> {
			const now = new Date().toISOString();
			const episode: Episode = {
				id: crypto.randomUUID(),
				title: input.title,
				description: input.description,
				status: "PLANNING" satisfies EpisodeStatus,
				guestId: input.guestId,
				guestName: input.guestId ? `Guest ${input.guestId.slice(0, 4)}` : "",
				audioUrl: "",
				showNotes: "",
				publishedAt: null,
				createdAt: now,
				updatedAt: now,
			};
			episodes = [...episodes, episode];
			return episode;
		},

		async updateEpisode(input: UpdateEpisodeInput): Promise<Episode> {
			const index = episodes.findIndex((e) => e.id === input.id);
			if (index === -1) {
				throw new Error(`Episode not found: ${input.id}`);
			}
			const existing = episodes[index];
			if (!existing) {
				throw new Error(`Episode not found: ${input.id}`);
			}
			const now = new Date().toISOString();
			const updated: Episode = {
				...existing,
				title: input.title ?? existing.title,
				description: input.description ?? existing.description,
				status: input.status ?? existing.status,
				showNotes: input.showNotes ?? existing.showNotes,
				publishedAt: input.status === "PUBLISHED" ? now : existing.publishedAt,
				updatedAt: now,
			};
			episodes = episodes.map((e) => (e.id === input.id ? updated : e));
			return updated;
		},

		async deleteEpisode(id: string): Promise<void> {
			episodes = episodes.filter((e) => e.id !== id);
		},
	};
}
