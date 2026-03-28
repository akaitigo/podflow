import { useCallback, useMemo, useState } from "react";
import { CreateEpisodeModal } from "./components/CreateEpisodeModal";
import { EpisodeDetailModal } from "./components/EpisodeDetailModal";
import { EpisodeListView } from "./components/EpisodeListView";
import { ErrorBanner } from "./components/ErrorBanner";
import { Header } from "./components/Header";
import headerStyles from "./components/Header.module.css";
import { KanbanBoard } from "./components/KanbanBoard";
import { useEpisodes } from "./hooks/useEpisodes";
import { useIsMobile } from "./hooks/useMediaQuery";
import { createMockApi } from "./lib/api";
import { MOCK_EPISODES } from "./lib/mock-data";
import type { CreateEpisodeInput, Episode, EpisodeStatus, UpdateEpisodeInput } from "./types/episode";

function App() {
	const api = useMemo(() => createMockApi([...MOCK_EPISODES]), []);
	const { episodes, loading, error, createEpisode, updateEpisode, deleteEpisode, clearError } = useEpisodes(api);

	const isMobile = useIsMobile();

	const [showCreateModal, setShowCreateModal] = useState(false);
	const [selectedEpisode, setSelectedEpisode] = useState<Episode | null>(null);

	const handleStatusChange = useCallback(
		(episodeId: string, newStatus: EpisodeStatus) => {
			updateEpisode({ id: episodeId, status: newStatus });
		},
		[updateEpisode],
	);

	const handleCreateSubmit = useCallback(
		(input: CreateEpisodeInput) => {
			createEpisode(input);
			setShowCreateModal(false);
		},
		[createEpisode],
	);

	const handleSaveDetail = useCallback(
		(input: UpdateEpisodeInput) => {
			updateEpisode(input);
			setSelectedEpisode(null);
		},
		[updateEpisode],
	);

	const handleDeleteEpisode = useCallback(
		(id: string) => {
			deleteEpisode(id);
			setSelectedEpisode(null);
		},
		[deleteEpisode],
	);

	const handleSelectEpisode = useCallback((episode: Episode) => {
		setSelectedEpisode(episode);
	}, []);

	return (
		<div>
			<Header onCreateClick={() => setShowCreateModal(true)} />
			{error && <ErrorBanner message={error} onDismiss={clearError} />}
			{loading ? (
				<div className={headerStyles.loading}>Loading episodes...</div>
			) : isMobile ? (
				<EpisodeListView
					episodes={episodes}
					onStatusChange={handleStatusChange}
					onSelectEpisode={handleSelectEpisode}
				/>
			) : (
				<KanbanBoard episodes={episodes} onStatusChange={handleStatusChange} onSelectEpisode={handleSelectEpisode} />
			)}
			{showCreateModal && (
				<CreateEpisodeModal onSubmit={handleCreateSubmit} onClose={() => setShowCreateModal(false)} />
			)}
			{selectedEpisode && (
				<EpisodeDetailModal
					episode={selectedEpisode}
					onSave={handleSaveDetail}
					onDelete={handleDeleteEpisode}
					onClose={() => setSelectedEpisode(null)}
				/>
			)}
		</div>
	);
}

export default App;
