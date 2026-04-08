import type { AuthResponse, LoginCredentials, RegisterPayload } from "../types/auth";
import { isAuthErrorResponse, isAuthResponse } from "../types/auth";

/**
 * Auth API client for login and registration.
 * Uses REST endpoints (not gRPC) since auth is handled via HTTP.
 */
export interface AuthApi {
	login(credentials: LoginCredentials): Promise<AuthResponse>;
	register(payload: RegisterPayload): Promise<AuthResponse>;
}

/** Create the auth API client. */
export function createAuthApi(baseUrl: string): AuthApi {
	async function request(path: string, body: Record<string, string>): Promise<AuthResponse> {
		const response = await fetch(`${baseUrl}${path}`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(body),
		});

		const data: unknown = await response.json();

		if (!response.ok) {
			if (isAuthErrorResponse(data)) {
				throw new Error(data.error);
			}
			throw new Error(`Authentication failed (${response.status})`);
		}

		if (!isAuthResponse(data)) {
			throw new Error("Unexpected response format from server");
		}

		return data;
	}

	return {
		async login(credentials: LoginCredentials): Promise<AuthResponse> {
			return request("/auth/login", {
				username: credentials.username,
				password: credentials.password,
			});
		},

		async register(payload: RegisterPayload): Promise<AuthResponse> {
			return request("/auth/register", {
				username: payload.username,
				password: payload.password,
				displayName: payload.displayName,
			});
		},
	};
}
