import styles from "./Header.module.css";

interface ErrorBannerProps {
	readonly message: string;
	readonly onDismiss: () => void;
}

export function ErrorBanner({ message, onDismiss }: ErrorBannerProps) {
	return (
		<div className={styles.errorBanner} role="alert">
			<span>{message}</span>
			<button type="button" className={styles.errorClose} onClick={onDismiss} aria-label="Dismiss error">
				&times;
			</button>
		</div>
	);
}
