import { useDraggable } from "@dnd-kit/core";
import type { Episode } from "../types/episode";
import styles from "./KanbanBoard.module.css";

interface EpisodeCardProps {
	readonly episode: Episode;
	readonly onSelect: (episode: Episode) => void;
}

function formatDate(iso: string): string {
	const date = new Date(iso);
	return date.toLocaleDateString("ja-JP", {
		month: "short",
		day: "numeric",
		hour: "2-digit",
		minute: "2-digit",
	});
}

export function EpisodeCard({ episode, onSelect }: EpisodeCardProps) {
	const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
		id: episode.id,
		data: { episode },
	});

	const className = [styles.card, isDragging ? styles.cardDragging : ""].filter(Boolean).join(" ");

	return (
		<div ref={setNodeRef} className={className} {...listeners} {...attributes}>
			<button type="button" className={styles.cardClickArea} onClick={() => onSelect(episode)}>
				<p className={styles.cardTitle}>{episode.title}</p>
				<div className={styles.cardMeta}>
					{episode.guestName && <span className={styles.cardGuest}>{episode.guestName}</span>}
					<span className={styles.cardDate}>{formatDate(episode.updatedAt)}</span>
				</div>
			</button>
		</div>
	);
}

/** Overlay shown during drag. */
export function EpisodeCardOverlay({ episode }: { readonly episode: Episode }) {
	return (
		<div className={styles.cardOverlay}>
			<p className={styles.cardTitle}>{episode.title}</p>
			<div className={styles.cardMeta}>
				{episode.guestName && <span className={styles.cardGuest}>{episode.guestName}</span>}
				<span className={styles.cardDate}>{formatDate(episode.updatedAt)}</span>
			</div>
		</div>
	);
}
