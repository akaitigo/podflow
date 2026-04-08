import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import App from "../App";
import { AuthProvider } from "../hooks/useAuth";

/** Render App wrapped with AuthProvider. Pre-seeds localStorage with a mock auth token. */
function renderApp() {
	return render(
		<AuthProvider>
			<App />
		</AuthProvider>,
	);
}

/** Set up a mock authenticated user in localStorage before each test. */
function seedAuthStorage() {
	localStorage.setItem("podflow_auth_token", "mock-test-token");
	localStorage.setItem("podflow_auth_user", JSON.stringify({ username: "testuser", displayName: "Test User" }));
}

afterEach(() => {
	cleanup();
	localStorage.clear();
});

beforeEach(() => {
	seedAuthStorage();
});

describe("App", () => {
	it("renders the heading and episode board header", async () => {
		renderApp();
		expect(screen.getByRole("heading", { name: /podflow/i })).toBeDefined();
		await waitFor(() => {
			expect(screen.getByText("Episode Board")).toBeDefined();
		});
	});

	it("shows the New Episode button", async () => {
		renderApp();
		await waitFor(() => {
			expect(screen.getByText("New Episode")).toBeDefined();
		});
	});

	it("displays episode cards after loading", async () => {
		renderApp();
		await waitFor(() => {
			expect(screen.getByText("The Future of AI in Podcasting")).toBeDefined();
		});
	});

	it("opens create modal when New Episode is clicked", async () => {
		const user = userEvent.setup();
		renderApp();

		await waitFor(() => {
			expect(screen.getByText("New Episode")).toBeDefined();
		});

		await user.click(screen.getByText("New Episode"));
		expect(screen.getByRole("dialog")).toBeDefined();
		expect(screen.getByLabelText(/title/i)).toBeDefined();
	});

	it("creates a new episode via the modal", async () => {
		const user = userEvent.setup();
		renderApp();

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
		renderApp();

		await waitFor(() => {
			expect(screen.getByText("The Future of AI in Podcasting")).toBeDefined();
		});

		await user.click(screen.getByText("The Future of AI in Podcasting"));

		await waitFor(() => {
			expect(screen.getByText("Episode Details")).toBeDefined();
		});
	});

	it("renders all six kanban columns", async () => {
		renderApp();

		await waitFor(() => {
			expect(screen.getByLabelText("Planning column")).toBeDefined();
			expect(screen.getByLabelText("Guest Coordination column")).toBeDefined();
			expect(screen.getByLabelText("Recording column")).toBeDefined();
			expect(screen.getByLabelText("Editing column")).toBeDefined();
			expect(screen.getByLabelText("Review column")).toBeDefined();
			expect(screen.getByLabelText("Published column")).toBeDefined();
		});
	});

	it("shows login page when not authenticated", async () => {
		localStorage.clear();
		renderApp();

		await waitFor(() => {
			expect(screen.getByText("Sign in to your account")).toBeDefined();
		});
	});

	it("shows logout button when authenticated", async () => {
		renderApp();

		await waitFor(() => {
			expect(screen.getByText("Logout")).toBeDefined();
		});
	});

	it("shows user display name when authenticated", async () => {
		renderApp();

		await waitFor(() => {
			expect(screen.getByText("Test User")).toBeDefined();
		});
	});
});
