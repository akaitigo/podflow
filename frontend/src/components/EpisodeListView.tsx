import { useMemo } from "react";
import { formatDate } from "../lib/format";
import type { Episode, EpisodeStatus } from "../types/episode";
import { EPISODE_STATUSES, STATUS_LABELS, canTransition, isEpisodeStatus } from "../types/episode";
import styles from "./KanbanBoard.module.css";

interface EpisodeListViewProps {
	readonly episodes: readonly Episode[];
	readonly onStatusChange: (episodeId: string, newStatus: EpisodeStatus) => void;
	readonly onSelectEpisode: (episode: Episode) => void;
}

function StatusSelector({
	episode,
	onStatusChange,
}: {
	readonly episode: Episode;
	readonly onStatusChange: (episodeId: string, newStatus: EpisodeStatus) => void;
}) {
	const availableStatuses = EPISODE_STATUSES.filter((s) => s === episode.status || canTransition(episode.status, s));

	if (availableStatuses.length <= 1) {
		return <span className={styles.listItemMeta}>{STATUS_LABELS[episode.status]}</span>;
	}

	return (
		<select
			className={styles.statusSelect}
			value={episode.status}
			onChange={(e) => {
				const value = e.target.value;
				if (isEpisodeStatus(value) && value !== episode.status) {
					onStatusChange(episode.id, value);
				}
			}}
			aria-label={`Change status for ${episode.title}`}
		>
			{availableStatuses.map((status) => (
				<option key={status} value={status}>
					{STATUS_LABELS[status]}
				</option>
			))}
		</select>
	);
}

export function EpisodeListView({ episodes, onStatusChange, onSelectEpisode }: EpisodeListViewProps) {
	const episodesByStatus = useMemo(() => {
		const grouped = new Map<EpisodeStatus, Episode[]>();
		for (const status of EPISODE_STATUSES) {
			grouped.set(status, []);
		}
		for (const episode of episodes) {
			const list = grouped.get(episode.status);
			if (list) {
				list.push(episode);
			}
		}
		return grouped;
	}, [episodes]);

	return (
		<section className={styles.list} aria-label="Episode list">
			{EPISODE_STATUSES.map((status) => {
				const items = episodesByStatus.get(status) ?? [];
				return (
					<div key={status} className={styles.listSection}>
						<div className={styles.listSectionHeader}>
							<span>
								{STATUS_LABELS[status]} ({items.length})
							</span>
						</div>
						{items.length === 0 ? (
							<p className={styles.empty}>No episodes</p>
						) : (
							<div className={styles.listSectionItems}>
								{items.map((episode) => (
									<div key={episode.id} className={styles.listItem}>
										<button type="button" className={styles.listItemButton} onClick={() => onSelectEpisode(episode)}>
											<span className={styles.listItemTitle}>{episode.title}</span>
											<span className={styles.listItemMeta}>
												{episode.guestName && `${episode.guestName} \u00B7 `}
												{formatDate(episode.updatedAt)}
											</span>
										</button>
										<StatusSelector episode={episode} onStatusChange={onStatusChange} />
									</div>
								))}
							</div>
						)}
					</div>
				);
			})}
		</section>
	);
}
