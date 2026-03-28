import { describe, expect, it } from "vitest";
import { createMockApi } from "../lib/api";
import type { Episode } from "../types/episode";

function makeSeedEpisode(overrides: Partial<Episode> = {}): Episode {
	return {
		id: "test-id",
		title: "Test Episode",
		description: "A test",
		status: "PLANNING",
		guestId: "",
		guestName: "",
		audioUrl: "",
		showNotes: "",
		publishedAt: null,
		createdAt: "2026-03-01T00:00:00Z",
		updatedAt: "2026-03-01T00:00:00Z",
		...overrides,
	};
}

describe("createMockApi", () => {
	it("lists episodes from seed data", async () => {
		const api = createMockApi([makeSeedEpisode()]);
		const episodes = await api.listEpisodes();
		expect(episodes).toHaveLength(1);
		expect(episodes[0]?.title).toBe("Test Episode");
	});

	it("creates a new episode", async () => {
		const api = createMockApi([]);
		const created = await api.createEpisode({
			title: "New Episode",
			description: "Desc",
			guestId: "",
		});
		expect(created.title).toBe("New Episode");
		expect(created.status).toBe("PLANNING");
		expect(created.id).toBeTruthy();

		const list = await api.listEpisodes();
		expect(list).toHaveLength(1);
	});

	it("updates an episode", async () => {
		const api = createMockApi([makeSeedEpisode({ id: "ep-1" })]);
		const updated = await api.updateEpisode({
			id: "ep-1",
			title: "Updated Title",
			status: "GUEST_COORDINATION",
		});
		expect(updated.title).toBe("Updated Title");
		expect(updated.status).toBe("GUEST_COORDINATION");
	});

	it("throws when updating a non-existent episode", async () => {
		const api = createMockApi([]);
		await expect(api.updateEpisode({ id: "nope", title: "x" })).rejects.toThrow("Episode not found");
	});

	it("deletes an episode", async () => {
		const api = createMockApi([makeSeedEpisode({ id: "ep-1" })]);
		await api.deleteEpisode("ep-1");
		const list = await api.listEpisodes();
		expect(list).toHaveLength(0);
	});

	it("sets publishedAt when status changes to PUBLISHED", async () => {
		const api = createMockApi([makeSeedEpisode({ id: "ep-1" })]);
		const updated = await api.updateEpisode({
			id: "ep-1",
			status: "PUBLISHED",
		});
		expect(updated.publishedAt).toBeTruthy();
	});
});
