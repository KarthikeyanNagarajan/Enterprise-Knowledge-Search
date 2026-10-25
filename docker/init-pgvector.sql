-- This runs automatically on first Postgres startup via docker-entrypoint-initdb.d and ensures the pgvector extension exists before Flyway migrations run. CREATE EXTENSION IF NOT EXISTS vector;
-- Ensures pgvector extension is available before Flyway migrations run.
CREATE EXTENSION IF NOT EXISTS vector;