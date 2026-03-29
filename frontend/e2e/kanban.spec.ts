import { expect, test } from "@playwright/test";

test.describe("Kanban Board", () => {
	test.beforeEach(async ({ page }) => {
		await page.goto("/");
	});

	test("displays the header with app name and new episode button", async ({ page }) => {
		await expect(page.getByRole("heading", { name: /podflow/i })).toBeVisible();
		await expect(page.getByText("Episode Board")).toBeVisible();
		await expect(page.getByRole("button", { name: /new episode/i })).toBeVisible();
	});

	test("renders all six kanban columns with episode counts", async ({ page }) => {
		await expect(page.getByLabel("Planning column")).toBeVisible();
		await expect(page.getByLabel("Guest Coordination column")).toBeVisible();
		await expect(page.getByLabel("Recording column")).toBeVisible();
		await expect(page.getByLabel("Editing column")).toBeVisible();
		await expect(page.getByLabel("Review column")).toBeVisible();
		await expect(page.getByLabel("Published column")).toBeVisible();
	});

	test("displays mock episode cards in the correct columns", async ({ page }) => {
		const planningColumn = page.getByLabel("Planning column");
		await expect(planningColumn.getByText("The Future of AI in Podcasting")).toBeVisible();
		await expect(planningColumn.getByText("Interview Techniques for Hosts")).toBeVisible();

		const guestColumn = page.getByLabel("Guest Coordination column");
		await expect(guestColumn.getByText("Remote Recording Best Practices")).toBeVisible();

		const publishedColumn = page.getByLabel("Published column");
		await expect(publishedColumn.getByText("Podcast SEO and Discoverability")).toBeVisible();
	});

	test("opens create episode modal and creates a new episode", async ({ page }) => {
		await page.getByRole("button", { name: /new episode/i }).click();

		const dialog = page.getByRole("dialog");
		await expect(dialog).toBeVisible();
		await expect(dialog.getByText("New Episode")).toBeVisible();

		await dialog.getByLabel("Title *").fill("My E2E Test Episode");
		await dialog.getByLabel("Description").fill("Created during E2E testing");
		await dialog.getByRole("button", { name: "Create Episode" }).click();

		await expect(dialog).not.toBeVisible();

		const planningColumn = page.getByLabel("Planning column");
		await expect(planningColumn.getByText("My E2E Test Episode")).toBeVisible();
	});

	test("opens episode detail modal when clicking a card", async ({ page }) => {
		const planningColumn = page.getByLabel("Planning column");
		await planningColumn.getByRole("button", { name: /The Future of AI in Podcasting/ }).click();

		const dialog = page.getByRole("dialog");
		await expect(dialog).toBeVisible();
		await expect(dialog.getByText("Episode Details")).toBeVisible();
		await expect(dialog.getByLabel("Title")).toHaveValue("The Future of AI in Podcasting");
		await expect(dialog.getByText("Alice Chen")).toBeVisible();
	});

	test("edits an episode title via the detail modal", async ({ page }) => {
		const planningColumn = page.getByLabel("Planning column");
		await planningColumn.getByRole("button", { name: /The Future of AI in Podcasting/ }).click();

		const dialog = page.getByRole("dialog");
		const titleInput = dialog.getByLabel("Title");
		await titleInput.clear();
		await titleInput.fill("Updated Episode Title");

		await dialog.getByRole("button", { name: "Save Changes" }).click();
		await expect(dialog).not.toBeVisible();

		await expect(planningColumn.getByText("Updated Episode Title")).toBeVisible();
	});

	test("closes the create modal with Cancel button", async ({ page }) => {
		await page.getByRole("button", { name: /new episode/i }).click();

		const dialog = page.getByRole("dialog");
		await expect(dialog).toBeVisible();

		await dialog.getByRole("button", { name: "Cancel" }).click();
		await expect(dialog).not.toBeVisible();
	});

	test("closes the detail modal with close button", async ({ page }) => {
		await page.getByText("The Future of AI in Podcasting").click();

		const dialog = page.getByRole("dialog");
		await expect(dialog).toBeVisible();

		await dialog.getByRole("button", { name: "Close" }).click();
		await expect(dialog).not.toBeVisible();
	});

	test("deletes an episode via the detail modal", async ({ page }) => {
		const planningColumn = page.getByLabel("Planning column");
		await planningColumn.getByRole("button", { name: /Interview Techniques for Hosts/ }).click();

		const dialog = page.getByRole("dialog");
		await dialog.getByRole("button", { name: "Delete" }).click();
		await dialog.getByRole("button", { name: "Confirm Delete" }).click();

		await expect(dialog).not.toBeVisible();
		await expect(planningColumn.getByText("Interview Techniques for Hosts")).not.toBeVisible();
	});

	test("shows status badge with correct label in detail modal", async ({ page }) => {
		const guestColumn = page.getByLabel("Guest Coordination column");
		await guestColumn.getByRole("button", { name: /Remote Recording Best Practices/ }).click();

		const dialog = page.getByRole("dialog");
		await expect(dialog.getByText("Guest Coordination")).toBeVisible();
	});
});
