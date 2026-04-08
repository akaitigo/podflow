import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { LoginPage } from "../components/LoginPage";
import { AuthProvider, useAuth } from "../hooks/useAuth";
import { isAuthErrorResponse, isAuthResponse } from "../types/auth";

afterEach(() => {
	cleanup();
	localStorage.clear();
});

describe("LoginPage", () => {
	it("renders login form by default", () => {
		render(
			<AuthProvider>
				<LoginPage />
			</AuthProvider>,
		);

		expect(screen.getByText("Sign in to your account")).toBeDefined();
		expect(screen.getByLabelText("Username")).toBeDefined();
		expect(screen.getByLabelText("Password")).toBeDefined();
		expect(screen.getByText("Sign In")).toBeDefined();
	});

	it("toggles to register mode", async () => {
		const user = userEvent.setup();
		render(
			<AuthProvider>
				<LoginPage />
			</AuthProvider>,
		);

		await user.click(screen.getByText("Don't have an account? Register"));

		expect(screen.getByText("Create your account")).toBeDefined();
		expect(screen.getByText("Create Account")).toBeDefined();
		expect(screen.getByLabelText("Display Name")).toBeDefined();
	});

	it("toggles back to login mode", async () => {
		const user = userEvent.setup();
		render(
			<AuthProvider>
				<LoginPage />
			</AuthProvider>,
		);

		await user.click(screen.getByText("Don't have an account? Register"));
		await user.click(screen.getByText("Already have an account? Sign in"));

		expect(screen.getByText("Sign in to your account")).toBeDefined();
	});

	it("disables submit button when fields are empty", () => {
		render(
			<AuthProvider>
				<LoginPage />
			</AuthProvider>,
		);

		const submitButton = screen.getByText("Sign In");
		expect(submitButton).toBeDefined();
		expect((submitButton as HTMLButtonElement).disabled).toBe(true);
	});

	it("mock login succeeds and stores token", async () => {
		const user = userEvent.setup();

		function TestApp() {
			const { user: authUser } = useAuth();
			if (authUser) {
				return <div data-testid="authenticated">Logged in as {authUser.username}</div>;
			}
			return <LoginPage />;
		}

		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await user.type(screen.getByLabelText("Username"), "testuser");
		await user.type(screen.getByLabelText("Password"), "password123");
		await user.click(screen.getByText("Sign In"));

		await waitFor(() => {
			expect(screen.getByTestId("authenticated")).toBeDefined();
		});

		expect(localStorage.getItem("podflow_auth_token")).toBe("mock-token");
	});

	it("mock register succeeds", async () => {
		const user = userEvent.setup();

		function TestApp() {
			const { user: authUser } = useAuth();
			if (authUser) {
				return <div data-testid="authenticated">Registered as {authUser.username}</div>;
			}
			return <LoginPage />;
		}

		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await user.click(screen.getByText("Don't have an account? Register"));
		await user.type(screen.getByLabelText("Username"), "newuser");
		await user.type(screen.getByLabelText("Password"), "password123");
		await user.type(screen.getByLabelText("Display Name"), "New User");
		await user.click(screen.getByText("Create Account"));

		await waitFor(() => {
			expect(screen.getByTestId("authenticated")).toBeDefined();
		});
	});

	it("restores auth state from localStorage on mount", async () => {
		localStorage.setItem("podflow_auth_token", "stored-token");
		localStorage.setItem("podflow_auth_user", JSON.stringify({ username: "stored", displayName: "Stored User" }));

		function TestApp() {
			const { user: authUser } = useAuth();
			if (authUser) {
				return <div data-testid="restored">Token: {authUser.token}</div>;
			}
			return <LoginPage />;
		}

		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("restored")).toBeDefined();
			expect(screen.getByText("Token: stored-token")).toBeDefined();
		});
	});
});

describe("mock mode production guard", () => {
	it("login throws in production when VITE_API_URL is not set", async () => {
		// Temporarily set DEV to false to simulate production
		const originalDev = import.meta.env.DEV;
		import.meta.env.DEV = false;

		let caughtError: Error | null = null;

		function TestApp() {
			const { login, user } = useAuth();
			if (user) {
				return <div data-testid="should-not-reach">Logged in</div>;
			}
			return (
				<button
					type="button"
					onClick={() => {
						login({ username: "test", password: "test123" }).catch((e: Error) => {
							caughtError = e;
						});
					}}
				>
					Try Login
				</button>
			);
		}

		const user = userEvent.setup();
		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await user.click(screen.getByText("Try Login"));
		await waitFor(() => {
			expect(caughtError).not.toBeNull();
			expect(caughtError?.message).toBe("VITE_API_URL is not configured. Cannot authenticate in production.");
		});

		import.meta.env.DEV = originalDev;
	});

	it("register throws in production when VITE_API_URL is not set", async () => {
		const originalDev = import.meta.env.DEV;
		import.meta.env.DEV = false;

		let caughtError: Error | null = null;

		function TestApp() {
			const { register, user: authUser } = useAuth();
			if (authUser) {
				return <div data-testid="should-not-reach">Registered</div>;
			}
			return (
				<button
					type="button"
					onClick={() => {
						register({ username: "test", password: "test123", displayName: "Test" }).catch((e: Error) => {
							caughtError = e;
						});
					}}
				>
					Try Register
				</button>
			);
		}

		const user = userEvent.setup();
		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await user.click(screen.getByText("Try Register"));
		await waitFor(() => {
			expect(caughtError).not.toBeNull();
			expect(caughtError?.message).toBe("VITE_API_URL is not configured. Cannot register in production.");
		});

		import.meta.env.DEV = originalDev;
	});

	it("mock login succeeds in DEV mode when VITE_API_URL is not set", async () => {
		// DEV mode is true by default in vitest
		const user = userEvent.setup();

		function TestApp() {
			const { user: authUser } = useAuth();
			if (authUser) {
				return <div data-testid="mock-authenticated">Logged in as {authUser.username}</div>;
			}
			return <LoginPage />;
		}

		render(
			<AuthProvider>
				<TestApp />
			</AuthProvider>,
		);

		await user.type(screen.getByLabelText("Username"), "devuser");
		await user.type(screen.getByLabelText("Password"), "password123");
		await user.click(screen.getByText("Sign In"));

		await waitFor(() => {
			expect(screen.getByTestId("mock-authenticated")).toBeDefined();
		});
	});
});

describe("auth type guards", () => {
	it("isAuthResponse identifies valid responses", () => {
		expect(isAuthResponse({ token: "abc", username: "user", displayName: "User" })).toBe(true);
		expect(isAuthResponse({ error: "bad" })).toBe(false);
		expect(isAuthResponse(null)).toBe(false);
		expect(isAuthResponse("string")).toBe(false);
	});

	it("isAuthErrorResponse identifies error responses", () => {
		expect(isAuthErrorResponse({ error: "bad request" })).toBe(true);
		expect(isAuthErrorResponse({ token: "abc" })).toBe(false);
		expect(isAuthErrorResponse(null)).toBe(false);
	});
});
