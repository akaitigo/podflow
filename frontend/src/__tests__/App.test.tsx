import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import App from "../App";

afterEach(() => {
	cleanup();
});

describe("App", () => {
	it("renders the heading and episode board header", async () => {
		render(<App />);
		expect(screen.getByRole("heading", { name: /podflow/i })).toBeDefined();
		await waitFor(() => {
			expect(screen.getByText("Episode Board")).toBeDefined();
		});
	});

	it("shows the New Episode button", async () => {
		render(<App />);
		await waitFor(() => {
			expect(screen.getByText("New Episode")).toBeDefined();
		});
	});

	it("displays episode cards after loading", async () => {
		render(<App />);
		await waitFor(() => {
			expect(screen.getByText("The Future of AI in Podcasting")).toBeDefined();
		});
	});

	it("opens create modal when New Episode is clicked", async () => {
		const user = userEvent.setup();
		render(<App />);

		await waitFor(() => {
			expect(screen.getByText("New Episode")).toBeDefined();
		});

		await user.click(screen.getByText("New Episode"));
		expect(screen.getByRole("dialog")).toBeDefined();
		expect(screen.getByLabelText(/title/i)).toBeDefined();
	});

	it("creates a new episode via the modal", async () => {
		const user = userEvent.setup();
		render(<App />);

		await waitFor(() => {
			expect(screen.getByText("New Episode")).toBeDefined();
		});

		await user.click(screen.getByText("New Episode"));

		const titleInput = screen.getByLabelText("Title *");
		await user.type(titleInput, "My New Episode");

		const descInput = screen.getByLabelText("Description");
		await user.type(descInput, "A great episode");

		await user.click(screen.getByText("Create Episode"));

		await waitFor(() => {
			expect(screen.getByText("My New Episode")).toBeDefined();
		});
	});

	it("opens episode detail modal when a card is clicked", async () => {
		const user = userEvent.setup();
		render(<App />);

		await waitFor(() => {
			expect(screen.getByText("The Future of AI in Podcasting")).toBeDefined();
		});

		await user.click(screen.getByText("The Future of AI in Podcasting"));

		await waitFor(() => {
			expect(screen.getByText("Episode Details")).toBeDefined();
		});
	});

	it("renders all six kanban columns", async () => {
		render(<App />);

		await waitFor(() => {
			expect(screen.getByLabelText("Planning column")).toBeDefined();
			expect(screen.getByLabelText("Guest Coordination column")).toBeDefined();
			expect(screen.getByLabelText("Recording column")).toBeDefined();
			expect(screen.getByLabelText("Editing column")).toBeDefined();
			expect(screen.getByLabelText("Review column")).toBeDefined();
			expect(screen.getByLabelText("Published column")).toBeDefined();
		});
	});
});
