import { useDroppable } from "@dnd-kit/core";
import type { Episode, EpisodeStatus } from "../types/episode";
import { STATUS_LABELS } from "../types/episode";
import { EpisodeCard } from "./EpisodeCard";
import styles from "./KanbanBoard.module.css";

interface KanbanColumnProps {
	readonly status: EpisodeStatus;
	readonly episodes: readonly Episode[];
	readonly onSelectEpisode: (episode: Episode) => void;
}

export function KanbanColumn({ status, episodes, onSelectEpisode }: KanbanColumnProps) {
	const { setNodeRef, isOver } = useDroppable({
		id: status,
		data: { status },
	});

	const className = [styles.column, isOver ? styles.columnOver : ""].filter(Boolean).join(" ");

	return (
		<div ref={setNodeRef} className={className} aria-label={`${STATUS_LABELS[status]} column`}>
			<div className={styles.columnHeader}>
				<span className={styles.columnTitle}>{STATUS_LABELS[status]}</span>
				<span className={styles.columnCount}>{episodes.length}</span>
			</div>
			<ul className={styles.cardList}>
				{episodes.length === 0 && <p className={styles.empty}>No episodes</p>}
				{episodes.map((episode) => (
					<li key={episode.id}>
						<EpisodeCard episode={episode} onSelect={onSelectEpisode} />
					</li>
				))}
			</ul>
		</div>
	);
}
