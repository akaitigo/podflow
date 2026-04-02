-- Improve ORDER BY created_at DESC query performance for episode listing.
-- The listEpisodes endpoint always sorts by created_at desc.
CREATE INDEX idx_episodes_created_at ON episodes(created_at DESC);
