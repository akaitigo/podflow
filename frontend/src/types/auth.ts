/** Credentials for user login. */
export interface LoginCredentials {
	readonly username: string;
	readonly password: string;
}

/** Payload for user registration. */
export interface RegisterPayload {
	readonly username: string;
	readonly password: string;
	readonly displayName: string;
}

/** Response from login/register endpoints. */
export interface AuthResponse {
	readonly token: string;
	readonly username: string;
	readonly displayName: string;
}

/** Error response from auth endpoints. */
export interface AuthErrorResponse {
	readonly error: string;
}

/** Authenticated user state. */
export interface AuthUser {
	readonly token: string;
	readonly username: string;
	readonly displayName: string;
}

/** Type guard for AuthErrorResponse. */
export function isAuthErrorResponse(value: unknown): value is AuthErrorResponse {
	return (
		typeof value === "object" &&
		value !== null &&
		"error" in value &&
		typeof (value as Record<string, unknown>).error === "string"
	);
}

/** Type guard for AuthResponse. */
export function isAuthResponse(value: unknown): value is AuthResponse {
	return (
		typeof value === "object" &&
		value !== null &&
		"token" in value &&
		typeof (value as Record<string, unknown>).token === "string" &&
		"username" in value &&
		typeof (value as Record<string, unknown>).username === "string"
	);
}
