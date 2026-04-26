# Database Schema
**Project:** ARIA
**Version:** 1.0.0

All schema changes are managed by Flyway. Never alter the database manually.
Migration files live in `src/main/resources/db/migration/`.

---

## Entity Relationship Overview

```
user_settings (1) ──── (1) context_snapshot
     │
     └── owns all content below (single-user v1, user_id = 1 always)

notes (1) ──── (M) note_tags
notes (1) ──── (M) tasks         [linked_note_id FK]
ideas (standalone)
tasks (M) ──── (1) events        [optional: task can have a linked event]
events (standalone)
note_vectors                     [pgvector — one row per chunk]
```

---

## Migration Files

### V1__initial_schema.sql

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─────────────────────────────────────────
-- NOTES
-- ─────────────────────────────────────────
CREATE TABLE notes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(500),
    body        TEXT NOT NULL DEFAULT '',
    is_pinned   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE note_tags (
    note_id     UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (note_id, tag)
);

CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC);
CREATE INDEX idx_note_tags_tag ON note_tags(tag);

-- ─────────────────────────────────────────
-- IDEAS
-- ─────────────────────────────────────────
CREATE TABLE ideas (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content         TEXT NOT NULL,
    is_converted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ideas_created_at ON ideas(created_at DESC);

-- ─────────────────────────────────────────
-- TASKS
-- ─────────────────────────────────────────
CREATE TYPE task_status AS ENUM ('TODO', 'IN_PROGRESS', 'DONE');

CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(500) NOT NULL,
    due_date        DATE,
    status          task_status NOT NULL DEFAULT 'TODO',
    linked_note_id  UUID REFERENCES notes(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);

-- ─────────────────────────────────────────
-- EVENTS
-- ─────────────────────────────────────────
CREATE TYPE event_source AS ENUM ('MANUAL', 'AI_SUGGESTED');

CREATE TABLE events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title               VARCHAR(500) NOT NULL,
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ,
    description         TEXT,
    reminder_minutes    INTEGER DEFAULT 15,
    source              event_source NOT NULL DEFAULT 'MANUAL',
    reminder_sent       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_reminder ON events(start_time, reminder_sent)
    WHERE reminder_sent = FALSE;

-- ─────────────────────────────────────────
-- VECTOR STORE (note chunks for RAG)
-- ─────────────────────────────────────────
CREATE TABLE note_vectors (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    note_id         UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    chunk_index     INTEGER NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       vector(768),          -- nomic-embed-text dimension
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (note_id, chunk_index)
);

-- IVFFlat index for fast approximate nearest-neighbour search
-- lists = sqrt(number of rows) — rebuild when data grows significantly
CREATE INDEX idx_note_vectors_embedding
    ON note_vectors USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ─────────────────────────────────────────
-- CONTEXT SNAPSHOT
-- ─────────────────────────────────────────
CREATE TABLE context_snapshots (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auto_content        TEXT NOT NULL DEFAULT '',   -- AI-generated, overwritten weekly
    user_preferences    TEXT NOT NULL DEFAULT '',   -- User-editable, never auto-overwritten
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Only one snapshot ever exists (single-user app)
INSERT INTO context_snapshots (auto_content, user_preferences)
VALUES ('', '');

-- ─────────────────────────────────────────
-- USER SETTINGS
-- ─────────────────────────────────────────
CREATE TABLE user_settings (
    id                      INTEGER PRIMARY KEY DEFAULT 1,   -- always row 1
    ai_provider             VARCHAR(50) NOT NULL DEFAULT 'ollama',
    ollama_model            VARCHAR(100) DEFAULT 'llama3.2',
    ollama_embedding_model  VARCHAR(100) DEFAULT 'nomic-embed-text',
    openai_api_key          VARCHAR(200),
    anthropic_api_key       VARCHAR(200),
    briefing_time           TIME NOT NULL DEFAULT '08:00:00',
    weekly_summary_time     TIME NOT NULL DEFAULT '18:00:00',
    onboarding_completed    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO user_settings DEFAULT VALUES;
```

---

### V2__add_task_linked_event.sql

```sql
-- Allow a task to reference the event that was scheduled for it
ALTER TABLE tasks
    ADD COLUMN linked_event_id UUID REFERENCES events(id) ON DELETE SET NULL;
```

---

### V3__add_full_text_search.sql

```sql
-- Full-text search index on notes for fast keyword search
ALTER TABLE notes ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))
    ) STORED;

CREATE INDEX idx_notes_fts ON notes USING GIN(search_vector);

-- Full-text search on ideas
ALTER TABLE ideas ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        to_tsvector('english', content)
    ) STORED;

CREATE INDEX idx_ideas_fts ON ideas USING GIN(search_vector);
```

---

## JPA Entity Notes

### Embedding dimension
The `note_vectors.embedding` column uses dimension 768, which matches `nomic-embed-text`.
If switching to OpenAI's `text-embedding-3-small`, dimension is 1536 — requires a migration:

```sql
-- V4__resize_embedding_dimension.sql (only run if switching embedding models)
ALTER TABLE note_vectors DROP COLUMN embedding;
ALTER TABLE note_vectors ADD COLUMN embedding vector(1536);
DROP INDEX IF EXISTS idx_note_vectors_embedding;
CREATE INDEX idx_note_vectors_embedding
    ON note_vectors USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

### Autosave strategy
Notes are saved client-side with a 1-second debounce. On the backend, `updated_at` is maintained via a JPA `@PreUpdate` hook:

```java
@PreUpdate
public void onUpdate() {
    this.updatedAt = Instant.now();
}
```

### Soft deletes (future consideration)
v1 uses hard deletes for simplicity. If a recycle bin feature is added later, add:

```sql
ALTER TABLE notes ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE tasks ADD COLUMN deleted_at TIMESTAMPTZ;
```

And filter queries with `WHERE deleted_at IS NULL`.

---

## Indexing Strategy Summary

| Table | Index | Purpose |
|---|---|---|
| notes | updated_at DESC | Default sort (newest first) |
| notes | GIN(search_vector) | Full-text keyword search |
| note_tags | tag | Filter notes by tag |
| ideas | created_at DESC | Default sort |
| tasks | status | Filter by TODO/DONE |
| tasks | due_date | Find overdue / upcoming tasks |
| events | start_time | Calendar range queries |
| events | (start_time, reminder_sent) | Efficient reminder polling |
| note_vectors | IVFFlat(embedding) | Fast vector similarity search |
