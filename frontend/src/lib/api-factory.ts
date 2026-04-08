import type { EpisodeApi } from "./api";
import { createMockApi } from "./api";
import { createGrpcApi } from "./grpc-api";
import { MOCK_EPISODES } from "./mock-data";

/**
 * Create the appropriate EpisodeApi implementation based on the environment.
 *
 * - When `VITE_API_URL` is set, returns a gRPC-Web client targeting that URL.
 * - Otherwise, returns an in-memory mock for local development.
 *
 * @param getToken - Optional function to provide the auth token for gRPC requests.
 */
export function createApiClient(getToken?: () => string | null): EpisodeApi {
	const apiUrl = import.meta.env.VITE_API_URL;
	if (apiUrl) {
		return createGrpcApi(apiUrl, getToken);
	}
	return createMockApi([...MOCK_EPISODES]);
}
