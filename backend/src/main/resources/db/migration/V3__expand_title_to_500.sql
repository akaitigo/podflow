-- Align title column length with MAX_TITLE_LENGTH = 500 enforced in application code.
-- Previously VARCHAR(255) could cause DB constraint violations for titles between 256-500 chars.
ALTER TABLE episodes ALTER COLUMN title TYPE VARCHAR(500);
