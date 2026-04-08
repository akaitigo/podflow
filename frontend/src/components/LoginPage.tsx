import { type FormEvent, useCallback, useState } from "react";
import { useAuth } from "../hooks/useAuth";
import styles from "./LoginPage.module.css";

export function LoginPage() {
	const { login, register } = useAuth();
	const [isRegisterMode, setIsRegisterMode] = useState(false);
	const [username, setUsername] = useState("");
	const [password, setPassword] = useState("");
	const [displayName, setDisplayName] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [submitting, setSubmitting] = useState(false);

	const handleSubmit = useCallback(
		async (e: FormEvent) => {
			e.preventDefault();
			setError(null);
			setSubmitting(true);

			try {
				if (isRegisterMode) {
					await register({
						username: username.trim(),
						password,
						displayName: displayName.trim(),
					});
				} else {
					await login({
						username: username.trim(),
						password,
					});
				}
			} catch (err: unknown) {
				const message = err instanceof Error ? err.message : "Authentication failed";
				setError(message);
			} finally {
				setSubmitting(false);
			}
		},
		[isRegisterMode, username, password, displayName, login, register],
	);

	const toggleMode = useCallback(() => {
		setIsRegisterMode((prev) => !prev);
		setError(null);
	}, []);

	const isFormValid = isRegisterMode
		? username.trim().length > 0 && password.length >= 8
		: username.trim().length > 0 && password.length > 0;

	return (
		<div className={styles.container}>
			<div className={styles.card}>
				<div className={styles.header}>
					<h1 className={styles.logo}>podflow</h1>
					<p className={styles.subtitle}>{isRegisterMode ? "Create your account" : "Sign in to your account"}</p>
				</div>

				{error && (
					<div className={styles.error} role="alert">
						{error}
					</div>
				)}

				<form onSubmit={handleSubmit} className={styles.form}>
					<div className={styles.field}>
						<label htmlFor="login-username" className={styles.label}>
							Username
						</label>
						<input
							id="login-username"
							className={styles.input}
							type="text"
							value={username}
							onChange={(e) => setUsername(e.target.value)}
							placeholder="Enter your username"
							maxLength={100}
							autoComplete="username"
							required
						/>
					</div>

					<div className={styles.field}>
						<label htmlFor="login-password" className={styles.label}>
							Password
						</label>
						<input
							id="login-password"
							className={styles.input}
							type="password"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							placeholder={isRegisterMode ? "At least 8 characters" : "Enter your password"}
							minLength={isRegisterMode ? 8 : undefined}
							autoComplete={isRegisterMode ? "new-password" : "current-password"}
							required
						/>
					</div>

					{isRegisterMode && (
						<div className={styles.field}>
							<label htmlFor="login-display-name" className={styles.label}>
								Display Name
							</label>
							<input
								id="login-display-name"
								className={styles.input}
								type="text"
								value={displayName}
								onChange={(e) => setDisplayName(e.target.value)}
								placeholder="How you want to be called (optional)"
								maxLength={255}
								autoComplete="name"
							/>
						</div>
					)}

					<button type="submit" className={styles.submitButton} disabled={!isFormValid || submitting}>
						{submitting ? "Please wait..." : isRegisterMode ? "Create Account" : "Sign In"}
					</button>
				</form>

				<div className={styles.footer}>
					<button type="button" className={styles.toggleButton} onClick={toggleMode}>
						{isRegisterMode ? "Already have an account? Sign in" : "Don't have an account? Register"}
					</button>
				</div>
			</div>
		</div>
	);
}
