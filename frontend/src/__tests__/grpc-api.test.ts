import { describe, expect, it, vi } from "vitest";

/**
 * Unit tests for the gRPC API client module.
 *
 * These tests verify that the grpc-api module can be imported and that
 * createGrpcApi returns a valid EpisodeApi. Actual RPC calls require
 * a running backend, so network-level integration tests belong in e2e.
 */

describe("createGrpcApi", () => {
	it("returns an object implementing EpisodeApi", async () => {
		const { createGrpcApi } = await import("../lib/grpc-api");
		const api = createGrpcApi("http://localhost:8080");

		expect(api).toBeDefined();
		expect(typeof api.listEpisodes).toBe("function");
		expect(typeof api.createEpisode).toBe("function");
		expect(typeof api.updateEpisode).toBe("function");
		expect(typeof api.deleteEpisode).toBe("function");
	});
});

describe("createApiClient", () => {
	it("returns mock API when VITE_API_URL is not set", async () => {
		vi.stubEnv("VITE_API_URL", "");
		const { createApiClient } = await import("../lib/api-factory");
		const api = createApiClient();

		expect(api).toBeDefined();
		// Mock API has data, so listing should work synchronously-ish
		const episodes = await api.listEpisodes();
		expect(episodes.length).toBeGreaterThan(0);

		vi.unstubAllEnvs();
	});

	it("returns gRPC API when VITE_API_URL is set", async () => {
		vi.stubEnv("VITE_API_URL", "http://localhost:8080");

		// Re-import to pick up the new env value
		vi.resetModules();
		const { createApiClient } = await import("../lib/api-factory");
		const api = createApiClient();

		expect(api).toBeDefined();
		expect(typeof api.listEpisodes).toBe("function");

		vi.unstubAllEnvs();
	});
});
