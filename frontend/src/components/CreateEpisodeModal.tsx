import { type FormEvent, useCallback, useRef, useState } from "react";
import type { CreateEpisodeInput } from "../types/episode";
import styles from "./Modal.module.css";

interface CreateEpisodeModalProps {
	readonly onSubmit: (input: CreateEpisodeInput) => void;
	readonly onClose: () => void;
}

export function CreateEpisodeModal({ onSubmit, onClose }: CreateEpisodeModalProps) {
	const [title, setTitle] = useState("");
	const [description, setDescription] = useState("");
	const titleRef = useRef<HTMLInputElement>(null);

	const handleSubmit = useCallback(
		(e: FormEvent) => {
			e.preventDefault();
			if (!title.trim()) return;
			onSubmit({
				title: title.trim(),
				description: description.trim(),
				guestId: "",
			});
		},
		[title, description, onSubmit],
	);

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

	return (
		<div className={styles.overlay} onClick={handleOverlayClick} onKeyDown={handleOverlayKeyDown} role="presentation">
			<dialog className={styles.dialog} open aria-labelledby="create-episode-title">
				<div className={styles.header}>
					<h2 id="create-episode-title" className={styles.title}>
						New Episode
					</h2>
					<button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close">
						&times;
					</button>
				</div>
				<form onSubmit={handleSubmit}>
					<div className={styles.body}>
						<div className={styles.form}>
							<div className={styles.field}>
								<label htmlFor="episode-title" className={styles.label}>
									Title *
								</label>
								<input
									ref={titleRef}
									id="episode-title"
									className={styles.input}
									type="text"
									value={title}
									onChange={(e) => setTitle(e.target.value)}
									placeholder="Episode title"
									required
								/>
							</div>
							<div className={styles.field}>
								<label htmlFor="episode-description" className={styles.label}>
									Description
								</label>
								<textarea
									id="episode-description"
									className={styles.textarea}
									value={description}
									onChange={(e) => setDescription(e.target.value)}
									placeholder="Brief description of the episode"
									rows={3}
								/>
							</div>
						</div>
					</div>
					<div className={styles.footer}>
						<button type="button" className={styles.buttonSecondary} onClick={onClose}>
							Cancel
						</button>
						<button type="submit" className={styles.buttonPrimary} disabled={!title.trim()}>
							Create Episode
						</button>
					</div>
				</form>
			</dialog>
		</div>
	);
}
