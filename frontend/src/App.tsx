import { useCallback, useMemo, useState } from "react";
import { CreateEpisodeModal } from "./components/CreateEpisodeModal";
import { EpisodeDetailModal } from "./components/EpisodeDetailModal";
import { EpisodeListView } from "./components/EpisodeListView";
import { ErrorBanner } from "./components/ErrorBanner";
import { Header } from "./components/Header";
import headerStyles from "./components/Header.module.css";
import { KanbanBoard } from "./components/KanbanBoard";
import { LoginPage } from "./components/LoginPage";
import { useAuth } from "./hooks/useAuth";
import { useEpisodes } from "./hooks/useEpisodes";
import { useIsMobile } from "./hooks/useMediaQuery";
import { createApiClient } from "./lib/api-factory";
import type { CreateEpisodeInput, Episode, EpisodeStatus, UpdateEpisodeInput } from "./types/episode";

function App() {
	const { user, loading: authLoading, logout } = useAuth();

	const api = useMemo(() => createApiClient(user ? () => user.token : undefined), [user]);
	const { episodes, loading, error, createEpisode, updateEpisode, deleteEpisode, clearError } = useEpisodes(api);

	const isMobile = useIsMobile();

	const [showCreateModal, setShowCreateModal] = useState(false);
	const [selectedEpisode, setSelectedEpisode] = useState<Episode | null>(null);

	const handleStatusChange = useCallback(
		(episodeId: string, newStatus: EpisodeStatus) => {
			updateEpisode({ id: episodeId, status: newStatus }).catch(() => {
				// Error state is already set by useEpisodes reducer
			});
		},
		[updateEpisode],
	);

	const handleCreateSubmit = useCallback(
		async (input: CreateEpisodeInput) => {
			try {
				await createEpisode(input);
				setShowCreateModal(false);
			} catch {
				// Error state is already set by useEpisodes reducer.
				// Keep modal open so the user can see the error and retry.
			}
		},
		[createEpisode],
	);

	const handleSaveDetail = useCallback(
		async (input: UpdateEpisodeInput) => {
			try {
				await updateEpisode(input);
				setSelectedEpisode(null);
			} catch {
				// Error state is already set by useEpisodes reducer.
				// Keep detail modal open so the user can see the error and retry.
			}
		},
		[updateEpisode],
	);

	const handleDeleteEpisode = useCallback(
		async (id: string) => {
			try {
				await deleteEpisode(id);
				setSelectedEpisode(null);
			} catch {
				// Error state is already set by useEpisodes reducer.
				// Keep detail modal open so the user can see the error and retry.
			}
		},
		[deleteEpisode],
	);

	const handleSelectEpisode = useCallback((episode: Episode) => {
		setSelectedEpisode(episode);
	}, []);

	if (authLoading) {
		return <div className={headerStyles.loading}>Loading...</div>;
	}

	if (!user) {
		return <LoginPage />;
	}

	return (
		<div>
			<Header onCreateClick={() => setShowCreateModal(true)} displayName={user.displayName} onLogout={logout} />
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
