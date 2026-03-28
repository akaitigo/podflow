/** Format an ISO date string to a short Japanese locale format. */
export function formatDate(iso: string): string {
	const date = new Date(iso);
	return date.toLocaleDateString("ja-JP", {
		month: "short",
		day: "numeric",
	});
}

/** Format an ISO date string with time to a Japanese locale format. */
export function formatDateTime(iso: string): string {
	const date = new Date(iso);
	return date.toLocaleDateString("ja-JP", {
		month: "short",
		day: "numeric",
		hour: "2-digit",
		minute: "2-digit",
	});
}
