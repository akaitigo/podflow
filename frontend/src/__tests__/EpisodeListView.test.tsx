import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EpisodeListView } from "../components/EpisodeListView";
import type { Episode } from "../types/episode";

function makeEpisode(overrides: Partial<Episode> = {}): Episode {
	return {
		id: "ep-1",
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

afterEach(() => {
	cleanup();
});

describe("EpisodeListView", () => {
	it("renders all status sections", () => {
		render(<EpisodeListView episodes={[]} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		expect(screen.getByText(/Planning/)).toBeDefined();
		expect(screen.getByText(/Guest Coordination/)).toBeDefined();
		expect(screen.getByText(/Recording/)).toBeDefined();
		expect(screen.getByText(/Editing/)).toBeDefined();
		expect(screen.getByText(/Review/)).toBeDefined();
		expect(screen.getByText(/Published/)).toBeDefined();
	});

	it("shows 'No episodes' for empty sections", () => {
		render(<EpisodeListView episodes={[]} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		const noEpisodesMessages = screen.getAllByText("No episodes");
		expect(noEpisodesMessages.length).toBe(6);
	});

	it("displays episodes grouped by status", () => {
		const episodes = [
			makeEpisode({ id: "ep-1", title: "Planning Episode", status: "PLANNING" }),
			makeEpisode({ id: "ep-2", title: "Editing Episode", status: "EDITING" }),
		];

		render(<EpisodeListView episodes={episodes} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		expect(screen.getByText("Planning Episode")).toBeDefined();
		expect(screen.getByText("Editing Episode")).toBeDefined();
	});

	it("shows episode count in section headers", () => {
		const episodes = [
			makeEpisode({ id: "ep-1", title: "Episode 1", status: "PLANNING" }),
			makeEpisode({ id: "ep-2", title: "Episode 2", status: "PLANNING" }),
		];

		render(<EpisodeListView episodes={episodes} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		expect(screen.getByText("Planning (2)")).toBeDefined();
	});

	it("calls onSelectEpisode when clicking an episode", async () => {
		const user = userEvent.setup();
		const onSelectEpisode = vi.fn();
		const episode = makeEpisode({ id: "ep-1", title: "Clickable Episode" });

		render(<EpisodeListView episodes={[episode]} onStatusChange={vi.fn()} onSelectEpisode={onSelectEpisode} />);

		await user.click(screen.getByText("Clickable Episode"));
		expect(onSelectEpisode).toHaveBeenCalledWith(episode);
	});

	it("displays guest name when present", () => {
		const episode = makeEpisode({
			id: "ep-1",
			title: "Guest Episode",
			guestName: "Alice Chen",
		});

		render(<EpisodeListView episodes={[episode]} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		expect(screen.getByText(/Alice Chen/)).toBeDefined();
	});

	it("calls onStatusChange when selecting a new status", async () => {
		const user = userEvent.setup();
		const onStatusChange = vi.fn();
		const episode = makeEpisode({ id: "ep-1", title: "Status Test", status: "PLANNING" });

		render(<EpisodeListView episodes={[episode]} onStatusChange={onStatusChange} onSelectEpisode={vi.fn()} />);

		const select = screen.getByLabelText("Change status for Status Test");
		await user.selectOptions(select, "RECORDING");
		expect(onStatusChange).toHaveBeenCalledWith("ep-1", "RECORDING");
	});

	it("has correct aria-label on the list section", () => {
		render(<EpisodeListView episodes={[]} onStatusChange={vi.fn()} onSelectEpisode={vi.fn()} />);

		expect(screen.getByLabelText("Episode list")).toBeDefined();
	});
});
