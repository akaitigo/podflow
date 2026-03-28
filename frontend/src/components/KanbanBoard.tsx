import {
	DndContext,
	type DragEndEvent,
	DragOverlay,
	type DragStartEvent,
	KeyboardSensor,
	MouseSensor,
	TouchSensor,
	useSensor,
	useSensors,
} from "@dnd-kit/core";
import { useCallback, useMemo, useState } from "react";
import type { Episode, EpisodeStatus } from "../types/episode";
import { EPISODE_STATUSES, canTransition, isEpisodeStatus } from "../types/episode";
import { EpisodeCardOverlay } from "./EpisodeCard";
import styles from "./KanbanBoard.module.css";
import { KanbanColumn } from "./KanbanColumn";

interface KanbanBoardProps {
	readonly episodes: readonly Episode[];
	readonly onStatusChange: (episodeId: string, newStatus: EpisodeStatus) => void;
	readonly onSelectEpisode: (episode: Episode) => void;
}

export function KanbanBoard({ episodes, onStatusChange, onSelectEpisode }: KanbanBoardProps) {
	const [activeEpisode, setActiveEpisode] = useState<Episode | null>(null);

	const sensors = useSensors(
		useSensor(MouseSensor, { activationConstraint: { distance: 8 } }),
		useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 5 } }),
		useSensor(KeyboardSensor),
	);

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

	const handleDragStart = useCallback(
		(event: DragStartEvent) => {
			const episodeId = event.active.id;
			const episode = episodes.find((e) => e.id === episodeId);
			if (episode) {
				setActiveEpisode(episode);
			}
		},
		[episodes],
	);

	const handleDragEnd = useCallback(
		(event: DragEndEvent) => {
			setActiveEpisode(null);

			const { active, over } = event;
			if (!over) return;

			const episodeId = String(active.id);
			const targetStatusStr = String(over.id);
			if (!isEpisodeStatus(targetStatusStr)) return;

			const episode = episodes.find((e) => e.id === episodeId);

			if (!episode) return;
			if (episode.status === targetStatusStr) return;
			if (!canTransition(episode.status, targetStatusStr)) return;

			onStatusChange(episodeId, targetStatusStr);
		},
		[episodes, onStatusChange],
	);

	return (
		<DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
			<section className={styles.board} aria-label="Kanban board">
				{EPISODE_STATUSES.map((status) => (
					<KanbanColumn
						key={status}
						status={status}
						episodes={episodesByStatus.get(status) ?? []}
						onSelectEpisode={onSelectEpisode}
					/>
				))}
			</section>
			<DragOverlay>{activeEpisode ? <EpisodeCardOverlay episode={activeEpisode} /> : null}</DragOverlay>
		</DndContext>
	);
}
