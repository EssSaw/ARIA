# Product Requirements Document
**Project:** ARIA — AI-powered Personal Knowledge & Schedule Manager
**Version:** 1.0.0
**Status:** Draft
**Author:** Dũng (Owner)
**Last updated:** 2025

---

## 1. Overview

ARIA is a local-first desktop application that unifies note-taking, task management, event scheduling, and idea capture into a single workspace, with an embedded AI assistant that understands the user's full context. Unlike cloud-heavy tools, ARIA runs entirely on the user's machine, keeping data private and accessible offline.

---

## 2. Problem Statement

Users currently suffer from:
- **Fragmentation** — notes in one app, calendar in another, tasks in a third
- **Context loss** — AI assistants that don't know anything about the user's actual life
- **Passive tools** — apps that store data but never help the user think or plan
- **Retrieval friction** — difficulty finding things written weeks or months ago

ARIA solves all four by being one place with an AI that knows everything in it.

---

## 3. Goals

| Goal | Description |
|---|---|
| Unified workspace | One app for notes, ideas, tasks, and events |
| AI that knows you | AI chat grounded in the user's actual stored content (RAG) |
| AI-driven scheduling | User can prompt the AI to create and modify calendar events |
| Proactive summaries | Daily briefing and weekly review generated automatically |
| Local-first | All data stored on-device; no cloud dependency |
| Provider-agnostic AI | Works with Ollama (local), OpenAI, or Anthropic — user's choice |
| Context portability | AI maintains a context snapshot document so switching providers preserves continuity |

---

## 4. Non-Goals (v1.0)

- No mobile app (considered for a future version)
- No real-time collaboration or multi-user support
- No cloud sync (architecture should not block adding this later)
- No voice input
- No file/attachment uploads
- No integrations with external calendars (Google Calendar, Outlook) in v1

---

## 5. User Persona

**Primary user: Dũng (Developer / Power User)**
- Technically proficient, comfortable with terminal and local setup
- Works across multiple projects simultaneously
- Tends to write in plain text / free-form
- Values privacy — does not want personal data leaving the machine
- Wants to build the app themselves, so needs it to be maintainable and extensible

---

## 6. Content Types

### 6.1 Note
A free-form written document. The most flexible content type.

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| title | String | Optional — auto-generated from first line if empty |
| body | Text | Plain text, no forced structure |
| tags | String[] | User-defined labels |
| created_at | Timestamp | Set on creation |
| updated_at | Timestamp | Updated on every edit |
| is_pinned | Boolean | Pinned notes appear at top of list |

### 6.2 Idea
A quick-capture content type. No structure required — just a thought, saved instantly.

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| content | Text | The idea, free-form |
| created_at | Timestamp | Auto-set |
| is_converted | Boolean | True if promoted to a Note or Task |

### 6.3 Task
A unit of work with a status and optional due date.

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| title | String | Required |
| due_date | Date | Optional |
| status | Enum | TODO, IN_PROGRESS, DONE |
| linked_note_id | UUID FK | Optional link to a Note |
| created_at | Timestamp | Auto-set |

### 6.4 Event
A calendar entry with a time, description, and reminder.

| Field | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| title | String | Required |
| start_time | Timestamp | Required |
| end_time | Timestamp | Optional |
| description | Text | Optional |
| reminder_minutes | Integer | Minutes before event to trigger reminder |
| source | Enum | MANUAL, AI_SUGGESTED |
| created_at | Timestamp | Auto-set |

---

## 7. Feature Requirements

### F-01: Note Management
**User story:** As a user, I want to create, edit, and delete notes in plain text so I can capture anything without friction.

**Acceptance criteria:**
- I can create a note with a title and body (title optional)
- I can add and remove tags from a note
- I can pin a note to the top of the list
- Notes are saved automatically as I type (autosave, 1-second debounce)
- I can search notes by title, body content, or tag
- I can delete a note with a confirmation prompt

---

### F-02: Idea Quick Capture
**User story:** As a user, I want to jot down an idea in one click, without filling in any form.

**Acceptance criteria:**
- A persistent "quick capture" input bar is always accessible on the dashboard
- Pressing Enter saves the idea immediately
- Ideas appear in an "Ideas" feed sorted by newest first
- I can promote an idea to a Note or a Task from the feed
- I can delete ideas

---

### F-03: Task Management
**User story:** As a user, I want to track things I need to do, with optional due dates.

**Acceptance criteria:**
- I can create a task with a title and optional due date
- I can change a task's status (TODO → IN_PROGRESS → DONE)
- I can link a task to an existing note for context
- Overdue tasks are visually highlighted on the dashboard
- I can filter tasks by status

---

### F-04: Event Calendar
**User story:** As a user, I want to see my schedule in a calendar view and manage events.

**Acceptance criteria:**
- I can view events in a monthly and weekly calendar layout
- I can create events manually with title, start/end time, description, and reminder
- I can edit and delete events
- Events created by the AI are visually distinguished (e.g. "AI suggested" badge)
- Reminders trigger an in-app notification at the specified time before the event

---

### F-05: AI Chat (RAG-powered)
**User story:** As a user, I want to ask questions about my notes and get answers grounded in what I've actually written.

**Acceptance criteria:**
- There is a chat panel accessible from the dashboard
- Messages stream word-by-word (SSE), not all at once
- The AI's answers cite which note(s) they are based on
- I can ask broad questions ("what are my ideas about project X?") and get relevant summaries
- Chat history is preserved within a session
- The AI does not hallucinate content that isn't in my notes

---

### F-06: AI Scheduling
**User story:** As a user, I want the AI to add events to my calendar when I ask it to, and also proactively suggest a schedule based on my tasks and notes.

**Acceptance criteria:**
- I can type a natural language prompt like "block 2 hours for studying tomorrow afternoon"
- The AI parses the intent and shows a preview card: title, date/time, description
- I can edit any field on the preview before confirming
- On confirm, the event is added to the calendar
- The AI can also proactively suggest scheduling a task when it notices a task has a near due date
- I can dismiss AI suggestions without adding them

---

### F-07: Daily Briefing
**User story:** As a user, I want a morning summary of what's on my plate today.

**Acceptance criteria:**
- At a configurable time (default 8:00 AM), a briefing panel appears on the dashboard
- The briefing includes: today's events, overdue tasks, tasks due today, and one AI-generated insight from recent notes
- I can dismiss the briefing
- I can change the briefing time in settings

---

### F-08: Weekly Summary
**User story:** As a user, I want a weekly review of what I did and what's coming next week.

**Acceptance criteria:**
- Every Sunday at a configurable time (default 6:00 PM), a weekly summary is generated
- The summary shows: tasks completed this week, events that happened, tasks due next week, events coming next week, and an AI-generated narrative of the week
- The summary is saved as a read-only Note for future reference
- I can trigger the summary manually from the settings page

---

### F-09: AI Context Snapshot
**User story:** As a user, I want the AI to maintain a "context document" about me so that if I switch AI providers, the new model still knows my preferences and patterns.

**Acceptance criteria:**
- The system maintains a special `context_snapshot` document updated periodically (weekly or on demand)
- The snapshot includes: user's working style, frequently referenced topics, recurring patterns in notes, active projects
- When any AI call is made, the context snapshot is injected into the system prompt
- When the user switches AI providers in settings, the app confirms that the context snapshot will be carried over
- The user can view and manually edit the context snapshot from the settings page

---

### F-10: AI Provider Settings
**User story:** As a user, I want to choose which AI model powers the app.

**Acceptance criteria:**
- Settings page shows available providers: Ollama (local), OpenAI, Anthropic
- For Ollama: user selects the model name (dropdown pulled from `ollama list`)
- For OpenAI / Anthropic: user enters their API key (stored locally, not synced)
- Switching providers triggers a warning about re-embedding and context snapshot migration
- A "Test connection" button verifies the provider is reachable before saving

---

### F-11: Onboarding Flow
**User story:** As a first-time user, I want clear guidance on how to set up the app.

**Acceptance criteria:**
- On first launch, a setup wizard appears (3 steps)
- Step 1: Check Ollama installation — show status (installed / not found) with download link if missing
- Step 2: Select and pull a model (progress bar shown during download)
- Step 3: Brief tour of the dashboard layout
- After completion, wizard is dismissed and never shown again (stored in local config)

---

## 8. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | AI chat first token should appear within 2 seconds on local Ollama |
| Startup | App should be interactive within 3 seconds of launch |
| Storage | All data in local PostgreSQL; no writes outside the project directory without user permission |
| Privacy | No telemetry, no analytics, no external calls except to the user-configured AI provider |
| Resilience | App must be fully functional without internet (Ollama mode) |
| Extensibility | AI provider, storage backend, and notification system must be behind interfaces to allow future swapping |

---

## 9. Out of Scope (Future Versions)

- Mobile app (React Native / Expo)
- Cloud sync with encryption
- Google Calendar / Outlook integration
- Collaboration features
- File attachments on notes
- Voice input
