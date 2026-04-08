import { type ReactNode, createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { AuthApi } from "../lib/auth-api";
import { createAuthApi } from "../lib/auth-api";
import type { AuthUser, LoginCredentials, RegisterPayload } from "../types/auth";

const TOKEN_STORAGE_KEY = "podflow_auth_token";
const USER_STORAGE_KEY = "podflow_auth_user";

interface AuthContextValue {
	readonly user: AuthUser | null;
	readonly loading: boolean;
	readonly login: (credentials: LoginCredentials) => Promise<void>;
	readonly register: (payload: RegisterPayload) => Promise<void>;
	readonly logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredUser(): AuthUser | null {
	try {
		const token = localStorage.getItem(TOKEN_STORAGE_KEY);
		const userJson = localStorage.getItem(USER_STORAGE_KEY);
		if (token && userJson) {
			const parsed: unknown = JSON.parse(userJson);
			if (typeof parsed === "object" && parsed !== null && "username" in parsed && "displayName" in parsed) {
				const user = parsed as { username: string; displayName: string };
				return { token, username: user.username, displayName: user.displayName };
			}
		}
	} catch {
		// Corrupted storage data — clear it
		localStorage.removeItem(TOKEN_STORAGE_KEY);
		localStorage.removeItem(USER_STORAGE_KEY);
	}
	return null;
}

function saveUser(user: AuthUser): void {
	localStorage.setItem(TOKEN_STORAGE_KEY, user.token);
	localStorage.setItem(USER_STORAGE_KEY, JSON.stringify({ username: user.username, displayName: user.displayName }));
}

function clearStoredUser(): void {
	localStorage.removeItem(TOKEN_STORAGE_KEY);
	localStorage.removeItem(USER_STORAGE_KEY);
}

interface AuthProviderProps {
	readonly children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
	const [user, setUser] = useState<AuthUser | null>(null);
	const [loading, setLoading] = useState(true);

	const authApi: AuthApi | null = useMemo(() => {
		const apiUrl = import.meta.env.VITE_API_URL;
		if (apiUrl) {
			return createAuthApi(apiUrl);
		}
		return null;
	}, []);

	useEffect(() => {
		const stored = loadStoredUser();
		if (stored) {
			setUser(stored);
		}
		setLoading(false);
	}, []);

	const login = useCallback(
		async (credentials: LoginCredentials) => {
			if (!authApi) {
				// Mock mode: skip actual API call
				const mockUser: AuthUser = {
					token: "mock-token",
					username: credentials.username,
					displayName: credentials.username,
				};
				setUser(mockUser);
				saveUser(mockUser);
				return;
			}

			const response = await authApi.login(credentials);
			const authUser: AuthUser = {
				token: response.token,
				username: response.username,
				displayName: response.displayName,
			};
			setUser(authUser);
			saveUser(authUser);
		},
		[authApi],
	);

	const register = useCallback(
		async (payload: RegisterPayload) => {
			if (!authApi) {
				// Mock mode: skip actual API call
				const mockUser: AuthUser = {
					token: "mock-token",
					username: payload.username,
					displayName: payload.displayName || payload.username,
				};
				setUser(mockUser);
				saveUser(mockUser);
				return;
			}

			const response = await authApi.register(payload);
			const authUser: AuthUser = {
				token: response.token,
				username: response.username,
				displayName: response.displayName,
			};
			setUser(authUser);
			saveUser(authUser);
		},
		[authApi],
	);

	const logout = useCallback(() => {
		setUser(null);
		clearStoredUser();
	}, []);

	const value = useMemo(() => ({ user, loading, login, register, logout }), [user, loading, login, register, logout]);

	return <AuthContext value={value}>{children}</AuthContext>;
}

/** Hook to access auth state and actions. Throws if used outside AuthProvider. */
export function useAuth(): AuthContextValue {
	const context = useContext(AuthContext);
	if (!context) {
		throw new Error("useAuth must be used within an AuthProvider");
	}
	return context;
}

/** Returns the current auth token, or null if not authenticated. */
export function useAuthToken(): string | null {
	const { user } = useAuth();
	return user?.token ?? null;
}
