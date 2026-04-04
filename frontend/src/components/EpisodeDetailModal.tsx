import { type FormEvent, useCallback, useEffect, useState } from "react";
import type { Episode, EpisodeStatus, UpdateEpisodeInput } from "../types/episode";
import { EPISODE_STATUSES, STATUS_LABELS, canTransition, isEpisodeStatus } from "../types/episode";
import styles from "./Modal.module.css";

interface EpisodeDetailModalProps {
	readonly episode: Episode;
	readonly onSave: (input: UpdateEpisodeInput) => void | Promise<void>;
	readonly onDelete: (id: string) => void | Promise<void>;
	readonly onClose: () => void;
}

const STATUS_STYLE_MAP: Record<EpisodeStatus, string> = {
	PLANNING: styles.statusPlanning ?? "",
	GUEST_COORDINATION: styles.statusGuestCoordination ?? "",
	RECORDING: styles.statusRecording ?? "",
	EDITING: styles.statusEditing ?? "",
	REVIEW: styles.statusReview ?? "",
	PUBLISHED: styles.statusPublished ?? "",
};

export function EpisodeDetailModal({ episode, onSave, onDelete, onClose }: EpisodeDetailModalProps) {
	const [title, setTitle] = useState(episode.title);
	const [description, setDescription] = useState(episode.description);
	const [status, setStatus] = useState<EpisodeStatus>(episode.status);
	const [showNotes, setShowNotes] = useState(episode.showNotes);
	const [confirmDelete, setConfirmDelete] = useState(false);

	useEffect(() => {
		setTitle(episode.title);
		setDescription(episode.description);
		setStatus(episode.status);
		setShowNotes(episode.showNotes);
	}, [episode]);

	const availableStatuses = EPISODE_STATUSES.filter((s) => s === episode.status || canTransition(episode.status, s));

	const hasChanges =
		title !== episode.title ||
		description !== episode.description ||
		status !== episode.status ||
		showNotes !== episode.showNotes;

	const handleSubmit = useCallback(
		(e: FormEvent) => {
			e.preventDefault();
			if (!title.trim()) return;
			const input: UpdateEpisodeInput = {
				id: episode.id,
				...(title !== episode.title && { title: title.trim() }),
				...(description !== episode.description && { description: description.trim() }),
				...(status !== episode.status && { status }),
				...(showNotes !== episode.showNotes && { showNotes }),
			};
			onSave(input);
		},
		[episode, title, description, status, showNotes, onSave],
	);

	const handleDelete = useCallback(() => {
		if (confirmDelete) {
			onDelete(episode.id);
		} else {
			setConfirmDelete(true);
		}
	}, [confirmDelete, episode.id, onDelete]);

	const handleOverlayClick = useCallback(
		(e: React.MouseEvent) => {
			if (e.target === e.currentTarget) {
				onClose();
			}
		},
		[onClose],
	);

	const handleOverlayKeyDown = useCallback(
		(e: React.KeyboardEvent) => {
			if (e.key === "Escape") {
				onClose();
			}
		},
		[onClose],
	);

	const statusBadgeClass = [styles.statusBadge, STATUS_STYLE_MAP[episode.status]].filter(Boolean).join(" ");

	return (
		<div className={styles.overlay} onClick={handleOverlayClick} onKeyDown={handleOverlayKeyDown} role="presentation">
			<dialog className={styles.dialog} open aria-labelledby="episode-detail-title">
				<div className={styles.header}>
					<h2 id="episode-detail-title" className={styles.title}>
						Episode Details
					</h2>
					<button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close">
						&times;
					</button>
				</div>
				<form onSubmit={handleSubmit}>
					<div className={styles.body}>
						<div className={styles.detailGrid}>
							<div>
								<span className={statusBadgeClass}>{STATUS_LABELS[episode.status]}</span>
							</div>

							<div className={styles.field}>
								<label htmlFor="detail-title" className={styles.label}>
									Title
								</label>
								<input
									id="detail-title"
									className={styles.input}
									type="text"
									value={title}
									onChange={(e) => setTitle(e.target.value)}
									maxLength={500}
									required
								/>
							</div>

							<div className={styles.field}>
								<label htmlFor="detail-description" className={styles.label}>
									Description
								</label>
								<textarea
									id="detail-description"
									className={styles.textarea}
									value={description}
									onChange={(e) => setDescription(e.target.value)}
									maxLength={50000}
									rows={3}
								/>
							</div>

							<div className={styles.field}>
								<label htmlFor="detail-status" className={styles.label}>
									Status
								</label>
								<select
									id="detail-status"
									className={styles.select}
									value={status}
									onChange={(e) => {
										const value = e.target.value;
										if (isEpisodeStatus(value)) {
											setStatus(value);
										}
									}}
								>
									{availableStatuses.map((s) => (
										<option key={s} value={s}>
											{STATUS_LABELS[s]}
										</option>
									))}
								</select>
							</div>

							<div className={styles.field}>
								<label htmlFor="detail-shownotes" className={styles.label}>
									Show Notes (Markdown)
								</label>
								<textarea
									id="detail-shownotes"
									className={styles.textarea}
									value={showNotes}
									onChange={(e) => setShowNotes(e.target.value)}
									maxLength={50000}
									rows={6}
									placeholder="Write show notes in markdown..."
								/>
							</div>

							{episode.guestName && (
								<div className={styles.field}>
									<span className={styles.label}>Guest</span>
									<span>{episode.guestName}</span>
								</div>
							)}
						</div>
					</div>
					<div className={styles.footer}>
						<div className={styles.footerActions}>
							<div className={styles.footerLeft}>
								<button type="button" className={styles.buttonDanger} onClick={handleDelete}>
									{confirmDelete ? "Confirm Delete" : "Delete"}
								</button>
							</div>
							<div style={{ display: "flex", gap: "8px" }}>
								<button type="button" className={styles.buttonSecondary} onClick={onClose}>
									Cancel
								</button>
								<button type="submit" className={styles.buttonPrimary} disabled={!hasChanges || !title.trim()}>
									Save Changes
								</button>
							</div>
						</div>
					</div>
				</form>
			</dialog>
		</div>
	);
}
