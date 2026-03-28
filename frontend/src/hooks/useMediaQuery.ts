import { useEffect, useState } from "react";

/** Returns true when the viewport matches the given media query. */
export function useMediaQuery(query: string): boolean {
	const [matches, setMatches] = useState(() => {
		if (typeof window === "undefined") return false;
		return window.matchMedia(query).matches;
	});

	useEffect(() => {
		const mql = window.matchMedia(query);
		const handler = (event: MediaQueryListEvent) => setMatches(event.matches);
		mql.addEventListener("change", handler);
		setMatches(mql.matches);
		return () => mql.removeEventListener("change", handler);
	}, [query]);

	return matches;
}

/** Returns true when the viewport is at most 768px wide. */
export function useIsMobile(): boolean {
	return useMediaQuery("(max-width: 768px)");
}
