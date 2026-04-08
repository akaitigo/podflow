import styles from "./Header.module.css";

interface HeaderProps {
	readonly onCreateClick: () => void;
	readonly displayName?: string;
	readonly onLogout?: () => void;
}

export function Header({ onCreateClick, displayName, onLogout }: HeaderProps) {
	return (
		<header className={styles.header}>
			<div className={styles.brand}>
				<h1 className={styles.logo}>podflow</h1>
				<span className={styles.subtitle}>Episode Board</span>
			</div>
			<div className={styles.actions}>
				<button type="button" className={styles.addButton} onClick={onCreateClick}>
					<span className={styles.addIcon}>+</span>
					New Episode
				</button>
				{displayName && <span className={styles.userName}>{displayName}</span>}
				{onLogout && (
					<button type="button" className={styles.logoutButton} onClick={onLogout}>
						Logout
					</button>
				)}
			</div>
		</header>
	);
}
