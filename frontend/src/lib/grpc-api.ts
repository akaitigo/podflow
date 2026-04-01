import { create } from "@bufbuild/protobuf";
import type { Client } from "@connectrpc/connect";
import { createClient } from "@connectrpc/connect";
import { createGrpcWebTransport } from "@connectrpc/connect-web";
import type { Episode as ProtoEpisode } from "../gen/podflow/v1/episode_pb";
import { EpisodeSchema, EpisodeStatus as ProtoEpisodeStatus } from "../gen/podflow/v1/episode_pb";
import {
	CreateEpisodeRequestSchema,
	DeleteEpisodeRequestSchema,
	EpisodeService,
	ListEpisodesRequestSchema,
	UpdateEpisodeRequestSchema,
} from "../gen/podflow/v1/episode_service_pb";
import type { Episode, EpisodeStatus } from "../types/episode";
import type { EpisodeApi } from "./api";

/** Map from frontend status string to protobuf EpisodeStatus enum value. */
const STATUS_TO_PROTO: Record<EpisodeStatus, ProtoEpisodeStatus> = {
	PLANNING: ProtoEpisodeStatus.PLANNING,
	GUEST_COORDINATION: ProtoEpisodeStatus.GUEST_COORDINATION,
	RECORDING: ProtoEpisodeStatus.RECORDING,
	EDITING: ProtoEpisodeStatus.EDITING,
	REVIEW: ProtoEpisodeStatus.REVIEW,
	PUBLISHED: ProtoEpisodeStatus.PUBLISHED,
};

/** Map from protobuf EpisodeStatus enum value to frontend status string. */
const PROTO_TO_STATUS: Record<number, EpisodeStatus> = {
	[ProtoEpisodeStatus.PLANNING]: "PLANNING",
	[ProtoEpisodeStatus.GUEST_COORDINATION]: "GUEST_COORDINATION",
	[ProtoEpisodeStatus.RECORDING]: "RECORDING",
	[ProtoEpisodeStatus.EDITING]: "EDITING",
	[ProtoEpisodeStatus.REVIEW]: "REVIEW",
	[ProtoEpisodeStatus.PUBLISHED]: "PUBLISHED",
};

/** Convert a protobuf Timestamp to an ISO string, returning null if absent. */
function timestampToIso(ts: { seconds: bigint; nanos: number } | undefined): string | null {
	if (ts === undefined) {
		return null;
	}
	const millis = Number(ts.seconds) * 1000 + Math.floor(ts.nanos / 1_000_000);
	return new Date(millis).toISOString();
}

/** Convert a protobuf Episode to the frontend Episode type. */
function toFrontendEpisode(proto: ProtoEpisode): Episode {
	return {
		id: proto.id,
		title: proto.title,
		description: proto.description,
		status: PROTO_TO_STATUS[proto.status] ?? "PLANNING",
		guestId: proto.guestId,
		guestName: "",
		audioUrl: proto.audioUrl,
		showNotes: proto.showNotes,
		publishedAt: timestampToIso(proto.publishedAt),
		createdAt: timestampToIso(proto.createdAt) ?? new Date().toISOString(),
		updatedAt: timestampToIso(proto.updatedAt) ?? new Date().toISOString(),
	};
}

/**
 * Create an EpisodeApi implementation backed by a gRPC-Web connection.
 *
 * @param baseUrl - The base URL of the gRPC backend (e.g. "http://localhost:8080").
 */
export function createGrpcApi(baseUrl: string): EpisodeApi {
	const transport = createGrpcWebTransport({ baseUrl });
	const client: Client<typeof EpisodeService> = createClient(EpisodeService, transport);

	return {
		async listEpisodes(): Promise<readonly Episode[]> {
			const request = create(ListEpisodesRequestSchema, {});
			const response = await client.listEpisodes(request);
			return response.episodes.map(toFrontendEpisode);
		},

		async createEpisode(input): Promise<Episode> {
			const request = create(CreateEpisodeRequestSchema, {
				title: input.title,
				description: input.description,
				guestId: input.guestId,
			});
			const response = await client.createEpisode(request);
			if (response.episode === undefined) {
				throw new Error("Server returned empty episode in CreateEpisode response");
			}
			return toFrontendEpisode(response.episode);
		},

		async updateEpisode(input): Promise<Episode> {
			const paths: string[] = [];

			if (input.title !== undefined) paths.push("title");
			if (input.description !== undefined) paths.push("description");
			if (input.status !== undefined) paths.push("status");
			if (input.showNotes !== undefined) paths.push("show_notes");

			const request = create(UpdateEpisodeRequestSchema, {
				episode: create(EpisodeSchema, {
					id: input.id,
					...(input.title !== undefined && { title: input.title }),
					...(input.description !== undefined && { description: input.description }),
					...(input.status !== undefined && { status: STATUS_TO_PROTO[input.status] }),
					...(input.showNotes !== undefined && { showNotes: input.showNotes }),
				}),
				updateMask: { paths },
			});
			const response = await client.updateEpisode(request);
			if (response.episode === undefined) {
				throw new Error("Server returned empty episode in UpdateEpisode response");
			}
			return toFrontendEpisode(response.episode);
		},

		async deleteEpisode(id): Promise<void> {
			const request = create(DeleteEpisodeRequestSchema, { id });
			await client.deleteEpisode(request);
		},
	};
}
