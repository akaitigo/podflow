/// <reference types="vite/client" />

interface ImportMetaEnv {
	/** Base URL of the gRPC backend. When set, the app uses real API instead of mock. */
	readonly VITE_API_URL?: string;
}

interface ImportMeta {
	readonly env: ImportMetaEnv;
}
