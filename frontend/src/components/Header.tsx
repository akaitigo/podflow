import styles from "./Header.module.css";

interface HeaderProps {
	readonly onCreateClick: () => void;
}

export function Header({ onCreateClick }: HeaderProps) {
	return (
		<header className={styles.header}>
			<div className={styles.brand}>
				<h1 className={styles.logo}>podflow</h1>
				<span className={styles.subtitle}>Episode Board</span>
			</div>
			<button type="button" className={styles.addButton} onClick={onCreateClick}>
				<span className={styles.addIcon}>+</span>
				New Episode
			</button>
		</header>
	);
}
