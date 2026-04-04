import { useCallback, useEffect, useReducer } from "react";
import type { EpisodeApi } from "../lib/api";
import type { CreateEpisodeInput, Episode, UpdateEpisodeInput } from "../types/episode";

interface EpisodesState {
	readonly episodes: readonly Episode[];
	readonly loading: boolean;
	readonly error: string | null;
}

type EpisodesAction =
	| { readonly type: "FETCH_START" }
	| { readonly type: "FETCH_SUCCESS"; readonly episodes: readonly Episode[] }
	| { readonly type: "FETCH_ERROR"; readonly error: string }
	| { readonly type: "ADD_EPISODE"; readonly episode: Episode }
	| { readonly type: "UPDATE_EPISODE"; readonly episode: Episode }
	| { readonly type: "DELETE_EPISODE"; readonly id: string }
	| { readonly type: "SET_ERROR"; readonly error: string }
	| { readonly type: "CLEAR_ERROR" };

function episodesReducer(state: EpisodesState, action: EpisodesAction): EpisodesState {
	switch (action.type) {
		case "FETCH_START":
			return { ...state, loading: true, error: null };
		case "FETCH_SUCCESS":
			return { episodes: action.episodes, loading: false, error: null };
		case "FETCH_ERROR":
			return { ...state, loading: false, error: action.error };
		case "SET_ERROR":
			return { ...state, error: action.error };
		case "ADD_EPISODE":
			return { ...state, episodes: [...state.episodes, action.episode] };
		case "UPDATE_EPISODE":
			return {
				...state,
				episodes: state.episodes.map((e) => (e.id === action.episode.id ? action.episode : e)),
			};
		case "DELETE_EPISODE":
			return {
				...state,
				episodes: state.episodes.filter((e) => e.id !== action.id),
			};
		case "CLEAR_ERROR":
			return { ...state, error: null };
	}
}

const initialState: EpisodesState = {
	episodes: [],
	loading: true,
	error: null,
};

export interface UseEpisodesResult {
	readonly episodes: readonly Episode[];
	readonly loading: boolean;
	readonly error: string | null;
	readonly createEpisode: (input: CreateEpisodeInput) => Promise<void>;
	readonly updateEpisode: (input: UpdateEpisodeInput) => Promise<void>;
	readonly deleteEpisode: (id: string) => Promise<void>;
	readonly clearError: () => void;
}

export function useEpisodes(api: EpisodeApi): UseEpisodesResult {
	const [state, dispatch] = useReducer(episodesReducer, initialState);

	useEffect(() => {
		let cancelled = false;
		dispatch({ type: "FETCH_START" });
		api
			.listEpisodes()
			.then((episodes) => {
				if (!cancelled) {
					dispatch({ type: "FETCH_SUCCESS", episodes });
				}
			})
			.catch((err: unknown) => {
				if (!cancelled) {
					const message = err instanceof Error ? err.message : "Failed to fetch episodes";
					dispatch({ type: "FETCH_ERROR", error: message });
				}
			});
		return () => {
			cancelled = true;
		};
	}, [api]);

	const createEpisode = useCallback(
		async (input: CreateEpisodeInput) => {
			try {
				const episode = await api.createEpisode(input);
				dispatch({ type: "ADD_EPISODE", episode });
			} catch (err: unknown) {
				const message = err instanceof Error ? err.message : "Failed to create episode";
				dispatch({ type: "SET_ERROR", error: message });
				throw err;
			}
		},
		[api],
	);

	const updateEpisode = useCallback(
		async (input: UpdateEpisodeInput) => {
			try {
				const episode = await api.updateEpisode(input);
				dispatch({ type: "UPDATE_EPISODE", episode });
			} catch (err: unknown) {
				const message = err instanceof Error ? err.message : "Failed to update episode";
				dispatch({ type: "SET_ERROR", error: message });
				throw err;
			}
		},
		[api],
	);

	const deleteEpisode = useCallback(
		async (id: string) => {
			try {
				await api.deleteEpisode(id);
				dispatch({ type: "DELETE_EPISODE", id });
			} catch (err: unknown) {
				const message = err instanceof Error ? err.message : "Failed to delete episode";
				dispatch({ type: "SET_ERROR", error: message });
				throw err;
			}
		},
		[api],
	);

	const clearError = useCallback(() => {
		dispatch({ type: "CLEAR_ERROR" });
	}, []);

	return {
		episodes: state.episodes,
		loading: state.loading,
		error: state.error,
		createEpisode,
		updateEpisode,
		deleteEpisode,
		clearError,
	};
}
