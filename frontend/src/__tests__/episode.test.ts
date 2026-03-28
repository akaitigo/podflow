import { describe, expect, it } from "vitest";
import { EpisodeStatus, canTransition } from "../types/episode";

describe("canTransition", () => {
	it("allows PLANNING -> GUEST_COORDINATION", () => {
		expect(canTransition(EpisodeStatus.PLANNING, EpisodeStatus.GUEST_COORDINATION)).toBe(true);
	});

	it("allows PLANNING -> RECORDING", () => {
		expect(canTransition(EpisodeStatus.PLANNING, EpisodeStatus.RECORDING)).toBe(true);
	});

	it("forbids PLANNING -> EDITING", () => {
		expect(canTransition(EpisodeStatus.PLANNING, EpisodeStatus.EDITING)).toBe(false);
	});

	it("allows GUEST_COORDINATION -> RECORDING", () => {
		expect(canTransition(EpisodeStatus.GUEST_COORDINATION, EpisodeStatus.RECORDING)).toBe(true);
	});

	it("forbids GUEST_COORDINATION -> PLANNING", () => {
		expect(canTransition(EpisodeStatus.GUEST_COORDINATION, EpisodeStatus.PLANNING)).toBe(false);
	});

	it("allows RECORDING -> EDITING", () => {
		expect(canTransition(EpisodeStatus.RECORDING, EpisodeStatus.EDITING)).toBe(true);
	});

	it("allows EDITING -> REVIEW", () => {
		expect(canTransition(EpisodeStatus.EDITING, EpisodeStatus.REVIEW)).toBe(true);
	});

	it("allows REVIEW -> PUBLISHED", () => {
		expect(canTransition(EpisodeStatus.REVIEW, EpisodeStatus.PUBLISHED)).toBe(true);
	});

	it("allows REVIEW -> EDITING (send back)", () => {
		expect(canTransition(EpisodeStatus.REVIEW, EpisodeStatus.EDITING)).toBe(true);
	});

	it("forbids PUBLISHED -> any status", () => {
		expect(canTransition(EpisodeStatus.PUBLISHED, EpisodeStatus.PLANNING)).toBe(false);
		expect(canTransition(EpisodeStatus.PUBLISHED, EpisodeStatus.REVIEW)).toBe(false);
		expect(canTransition(EpisodeStatus.PUBLISHED, EpisodeStatus.EDITING)).toBe(false);
	});

	it("forbids skipping stages (PLANNING -> PUBLISHED)", () => {
		expect(canTransition(EpisodeStatus.PLANNING, EpisodeStatus.PUBLISHED)).toBe(false);
	});
});
