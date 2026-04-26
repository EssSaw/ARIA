# API Design Document
**Project:** ARIA
**Version:** 1.0.0
**Base URL:** `http://localhost:8080/api`

All endpoints return JSON. All timestamps use ISO 8601 format.
Error responses follow a consistent shape (see Section 2).

---

## 1. Authentication

v1 uses a simple local JWT. All endpoints except `/auth/**` and `/health` require a valid Bearer token.

### POST /auth/login
```json
// Request
{
  "password": "your_local_password"
}

// Response 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2025-01-16T08:00:00Z"
}

// Response 401
{
  "error": "INVALID_CREDENTIALS",
  "message": "Incorrect password"
}
```

### GET /health
```json
// Response 200 — used by frontend splash screen polling
{
  "status": "UP",
  "aiProvider": "ollama",
  "dbConnected": true
}
```

---

## 2. Standard Error Shape

Every error response uses this structure:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "details": {}  // optional, validation errors go here
}
```

Common error codes:

| Code | HTTP | Meaning |
|---|---|---|
| `NOT_FOUND` | 404 | Resource does not exist |
| `VALIDATION_ERROR` | 400 | Request body failed validation |
| `UNAUTHORIZED` | 401 | Missing or invalid JWT |
| `AI_UNAVAILABLE` | 503 | Ollama or cloud provider not reachable |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 3. Notes

### GET /notes
Returns all notes sorted by `updatedAt` descending.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `search` | string | Full-text keyword search |
| `tag` | string | Filter by tag (exact match) |
| `pinned` | boolean | If true, return only pinned notes |
| `page` | int | Page number (default 0) |
| `size` | int | Page size (default 20) |

```json
// Response 200
{
  "content": [
    {
      "id": "uuid",
      "title": "Meeting notes",
      "body": "Discussed the Q2 roadmap...",
      "tags": ["work", "meetings"],
      "isPinned": false,
      "createdAt": "2025-01-10T09:00:00Z",
      "updatedAt": "2025-01-10T09:45:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "currentPage": 0
}
```

### POST /notes
```json
// Request
{
  "title": "Meeting notes",       // optional
  "body": "Discussed the...",
  "tags": ["work", "meetings"],
  "isPinned": false
}

// Response 201
{
  "id": "uuid",
  "title": "Meeting notes",
  "body": "Discussed the...",
  "tags": ["work", "meetings"],
  "isPinned": false,
  "createdAt": "2025-01-10T09:00:00Z",
  "updatedAt": "2025-01-10T09:00:00Z"
}
```

### GET /notes/{id}
```json
// Response 200 — same shape as individual note object above
// Response 404 if not found
```

### PUT /notes/{id}
Full update. All fields required (use GET first, then modify).
```json
// Request — same shape as POST /notes
// Response 200 — updated note object
```

### PATCH /notes/{id}
Partial update. Only send fields you want to change. Used for autosave.
```json
// Request
{
  "body": "Updated content..."
}
// Response 200 — updated note object
```

### DELETE /notes/{id}
```json
// Response 204 No Content
// Response 404 if not found
```

---

## 4. Ideas

### GET /ideas
```json
// Query params: page, size
// Response 200
{
  "content": [
    {
      "id": "uuid",
      "content": "What if we used vector search for task suggestions?",
      "isConverted": false,
      "createdAt": "2025-01-10T10:30:00Z"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "currentPage": 0
}
```

### POST /ideas
```json
// Request
{
  "content": "What if we used vector search for task suggestions?"
}

// Response 201 — idea object
```

### DELETE /ideas/{id}
```json
// Response 204
```

### POST /ideas/{id}/convert
Promotes an idea to a Note or Task.
```json
// Request
{
  "convertTo": "NOTE"  // or "TASK"
}

// Response 201
{
  "type": "NOTE",
  "id": "new-note-uuid"  // ID of the newly created note or task
}
```

---

## 5. Tasks

### GET /tasks
```json
// Query params:
//   status: TODO | IN_PROGRESS | DONE
//   overdue: boolean (due_date < today AND status != DONE)
//   page, size

// Response 200
{
  "content": [
    {
      "id": "uuid",
      "title": "Implement RAG pipeline",
      "dueDate": "2025-01-20",
      "status": "IN_PROGRESS",
      "linkedNoteId": "note-uuid-or-null",
      "linkedEventId": "event-uuid-or-null",
      "isOverdue": false,
      "createdAt": "2025-01-10T08:00:00Z",
      "updatedAt": "2025-01-12T14:00:00Z"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "currentPage": 0
}
```

### POST /tasks
```json
// Request
{
  "title": "Implement RAG pipeline",
  "dueDate": "2025-01-20",         // optional, YYYY-MM-DD
  "linkedNoteId": "uuid"           // optional
}

// Response 201 — task object
```

### PATCH /tasks/{id}
```json
// Request — any subset of fields
{
  "status": "DONE"
}
// Response 200 — updated task object
```

### DELETE /tasks/{id}
```json
// Response 204
```

---

## 6. Events

### GET /events
```json
// Query params:
//   from: ISO date (e.g. 2025-01-01)
//   to:   ISO date (e.g. 2025-01-31)
//   source: MANUAL | AI_SUGGESTED

// Response 200
{
  "events": [
    {
      "id": "uuid",
      "title": "Deep work session",
      "startTime": "2025-01-15T14:00:00Z",
      "endTime": "2025-01-15T16:00:00Z",
      "description": "Focus block for RAG pipeline work",
      "reminderMinutes": 15,
      "source": "AI_SUGGESTED",
      "createdAt": "2025-01-10T09:00:00Z"
    }
  ]
}
```

### POST /events
```json
// Request
{
  "title": "Deep work session",
  "startTime": "2025-01-15T14:00:00Z",
  "endTime": "2025-01-15T16:00:00Z",  // optional
  "description": "...",                // optional
  "reminderMinutes": 15,               // optional, default 15
  "source": "MANUAL"
}

// Response 201 — event object
```

### PUT /events/{id}
```json
// Full update — same shape as POST
// Response 200 — updated event object
```

### DELETE /events/{id}
```json
// Response 204
```

---

## 7. AI Chat

### POST /chat
Non-streaming. Returns full response at once. Use for simple queries.
```json
// Request
{
  "message": "What have I been working on this week?"
}

// Response 200
{
  "reply": "Based on your notes, this week you worked on...",
  "sourcedFrom": [
    { "noteId": "uuid", "noteTitle": "Sprint planning" },
    { "noteId": "uuid", "noteTitle": "Daily log Jan 13" }
  ]
}
```

### GET /chat/stream
**SSE endpoint** — returns the response token by token. Use this for the main chat UI.

```
GET /api/chat/stream?message=What+did+I+plan+for+next+week
Authorization: Bearer <token>

// SSE events:
event: token
data: Based

event: token
data:  on

event: token
data:  your

event: source
data: {"noteId":"uuid","noteTitle":"Weekly plan"}

event: done
data: {}
```

React usage:
```js
const es = new EventSource(
  `/api/chat/stream?message=${encodeURIComponent(msg)}`,
  { headers: { Authorization: `Bearer ${token}` } }
);
es.addEventListener('token', e => appendToken(e.data));
es.addEventListener('done', () => es.close());
```

---

## 8. AI Scheduling

### POST /chat/schedule
Parses a natural language scheduling request and returns a preview.
Does NOT save the event — just returns a suggestion.

```json
// Request
{
  "prompt": "Schedule 2 hours for studying tomorrow afternoon"
}

// Response 200
{
  "preview": {
    "title": "Study session",
    "startTime": "2025-01-15T14:00:00Z",
    "endTime": "2025-01-15T16:00:00Z",
    "description": "2-hour focused study block",
    "confidence": 0.92
  },
  "conflict": null   // or { "eventId": "uuid", "title": "Existing meeting" }
}

// Response 422 — if the AI cannot parse the intent
{
  "error": "INTENT_PARSE_FAILED",
  "message": "Could not determine a clear time from your request. Try: 'tomorrow at 2pm for 1 hour'"
}
```

After the user reviews and edits the preview, call `POST /events` to save it.

---

## 9. Briefings

### GET /briefing/daily
Returns today's briefing. If it hasn't been generated yet (before 8 AM), generates it on demand.
```json
// Response 200
{
  "date": "2025-01-15",
  "generatedAt": "2025-01-15T08:00:00Z",
  "todaysEvents": [ /* event objects */ ],
  "overdueTasks": [ /* task objects */ ],
  "tasksDueToday": [ /* task objects */ ],
  "aiInsight": "You have a heavy day — consider moving the planning session to tomorrow.",
  "schedulingSuggestions": [
    {
      "taskId": "uuid",
      "taskTitle": "Review PR comments",
      "preview": {
        "title": "Review PR comments",
        "startTime": "2025-01-15T10:00:00Z",
        "endTime": "2025-01-15T10:30:00Z"
      }
    }
  ]
}
```

### GET /briefing/weekly
Returns this week's summary. Triggers generation if not yet done.
```json
// Response 200
{
  "weekStart": "2025-01-13",
  "weekEnd": "2025-01-19",
  "generatedAt": "2025-01-19T18:00:00Z",
  "completedTasks": [ /* task objects */ ],
  "pastEvents": [ /* event objects */ ],
  "upcomingTasks": [ /* task objects */ ],
  "upcomingEvents": [ /* event objects */ ],
  "aiNarrative": "This week you completed the auth module and started the RAG pipeline. Next week looks lighter — a good time to write tests.",
  "savedNoteId": "uuid"   // ID of the auto-saved weekly summary note
}
```

### POST /briefing/weekly/generate
Manually triggers weekly summary generation.
```json
// Response 202 Accepted
{
  "message": "Weekly summary generation started",
  "estimatedSeconds": 15
}
```

---

## 10. Notifications (SSE)

### GET /notifications/stream
Long-lived SSE connection. Frontend subscribes on load and listens for push events.

```
GET /api/notifications/stream
Authorization: Bearer <token>

// Event types:
event: reminder
data: {"eventId":"uuid","title":"Team sync","minutesUntil":15}

event: briefing_ready
data: {"type":"daily","date":"2025-01-15"}

event: weekly_summary_ready
data: {"savedNoteId":"uuid"}

event: ai_suggestion
data: {"type":"schedule","taskId":"uuid","taskTitle":"Write unit tests"}

event: reembed_progress
data: {"processed":42,"total":150,"percent":28}
```

---

## 11. Settings

### GET /settings
```json
// Response 200
{
  "aiProvider": "ollama",
  "ollamaModel": "llama3.2",
  "ollamaEmbeddingModel": "nomic-embed-text",
  "hasOpenAiKey": false,
  "hasAnthropicKey": false,
  "briefingTime": "08:00",
  "weeklySummaryTime": "18:00",
  "onboardingCompleted": true
}
```

### PATCH /settings
```json
// Request — any subset of settings
{
  "aiProvider": "openai",
  "openAiApiKey": "sk-..."
}

// Response 200 — updated settings (API keys masked)
// Response 503 if provider test fails
{
  "error": "AI_UNAVAILABLE",
  "message": "Could not connect to OpenAI with the provided API key"
}
```

### POST /settings/ai/test
Tests the currently saved AI provider connection.
```json
// Response 200
{
  "connected": true,
  "provider": "ollama",
  "model": "llama3.2",
  "latencyMs": 342
}
```

### GET /settings/ollama/models
Returns available models from the local Ollama instance.
```json
// Response 200
{
  "models": ["llama3.2", "mistral", "gemma2"]
}
// Response 503 if Ollama is not running
```

---

## 12. Context Snapshot

### GET /context-snapshot
```json
// Response 200
{
  "id": "uuid",
  "autoContent": "## About me\nI am a developer who...",
  "userPreferences": "I prefer concise answers...",
  "generatedAt": "2025-01-12T18:00:00Z"
}
```

### PATCH /context-snapshot/preferences
Updates only the user-editable preferences section.
```json
// Request
{
  "userPreferences": "I prefer concise answers. Always cite the source note."
}
// Response 200 — updated snapshot
```

### POST /context-snapshot/regenerate
Triggers a manual regeneration of the `autoContent` section.
```json
// Response 202 Accepted
{
  "message": "Context snapshot regeneration started"
}
// When done, a notification is pushed via /notifications/stream
```

---

## 13. Onboarding

### GET /onboarding/status
```json
// Response 200
{
  "completed": false,
  "steps": {
    "ollamaInstalled": true,
    "modelPulled": false,
    "tourCompleted": false
  }
}
```

### POST /onboarding/pull-model
Starts pulling the selected Ollama model. Progress streamed via `/notifications/stream`.
```json
// Request
{
  "model": "llama3.2"
}
// Response 202 Accepted
```

### POST /onboarding/complete
Marks onboarding as done. Called after the user completes the tour.
```json
// Response 200 { "completed": true }
```
