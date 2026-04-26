# Technical Design Document
**Project:** ARIA — AI-powered Personal Knowledge & Schedule Manager
**Version:** 1.0.0
**Status:** Draft

---

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     TAURI DESKTOP SHELL                 │
│                                                         │
│   ┌─────────────────────┐    ┌──────────────────────┐  │
│   │   React + Vite UI   │    │  Spring Boot Sidecar │  │
│   │                     │◄──►│                      │  │
│   │  - Dashboard        │    │  - REST API :8080    │  │
│   │  - Chat panel       │    │  - AI Service        │  │
│   │  - Calendar         │    │  - RAG Pipeline      │  │
│   │  - Notes editor     │    │  - Scheduler         │  │
│   │  - Settings         │    │  - Context Snapshot  │  │
│   └─────────────────────┘    └──────────┬───────────┘  │
│                                          │              │
└──────────────────────────────────────────│──────────────┘
                                           │
              ┌────────────────────────────┼────────────────────┐
              │                            │                    │
   ┌──────────▼──────────┐    ┌────────────▼──────┐  ┌─────────▼───────┐
   │  PostgreSQL + pgvec │    │  Ollama (local)   │  │ OpenAI/Anthropic│
   │  :5432              │    │  :11434           │  │ (cloud, optional)│
   └─────────────────────┘    └───────────────────┘  └─────────────────┘
```

---

## 2. Technology Stack

### Frontend
| Layer | Technology | Reason |
|---|---|---|
| Framework | React 18 + Vite | Fast dev experience, familiar |
| Routing | React Router v6 | Standard SPA routing |
| State management | Zustand | Lightweight, no boilerplate |
| HTTP client | Axios | Interceptors for auth headers |
| Calendar | react-big-calendar | Feature-rich, customizable |
| Styling | Tailwind CSS | Utility-first, rapid UI |
| Desktop shell | Tauri v2 | Native window, tiny bundle |

### Backend
| Layer | Technology | Reason |
|---|---|---|
| Framework | Spring Boot 3.3 | Familiar, production-grade |
| AI abstraction | Spring AI | Unified API for all LLM providers |
| ORM | Spring Data JPA (Hibernate) | Standard, well-known |
| Auth | Spring Security + JWT | Local auth, stateless |
| DB migrations | Flyway | Version-controlled schema changes |
| Scheduling | Spring `@Scheduled` | Built-in, no extra dependencies |
| Testing | JUnit 5 + Mockito | Standard Java testing stack |

### Data & AI
| Layer | Technology | Reason |
|---|---|---|
| Primary DB | PostgreSQL 16 | Reliable, supports pgvector |
| Vector store | pgvector extension | Keeps everything in one DB |
| Local LLM | Ollama (llama3.2) | Free, private, offline capable |
| Embedding model | nomic-embed-text | Fast, lightweight, good quality |
| Dev containers | Docker Compose | Reproducible local environment |

---

## 3. Module Breakdown (Backend)

```
com.aria
├── config/
│   ├── SecurityConfig.java          # JWT filter, CORS config
│   ├── SpringAiConfig.java          # ChatClient, EmbeddingModel beans
│   └── SchedulerConfig.java         # @EnableScheduling
│
├── domain/                          # JPA Entities
│   ├── Note.java
│   ├── Idea.java
│   ├── Task.java
│   ├── Event.java
│   ├── ContextSnapshot.java
│   └── UserSettings.java
│
├── repository/                      # Spring Data repos
│   ├── NoteRepository.java
│   ├── IdeaRepository.java
│   ├── TaskRepository.java
│   ├── EventRepository.java
│   └── ContextSnapshotRepository.java
│
├── dto/                             # Request/Response objects
│   ├── request/
│   └── response/
│
├── service/
│   ├── NoteService.java
│   ├── IdeaService.java
│   ├── TaskService.java
│   ├── EventService.java
│   ├── AiService.java               # Orchestrates all AI operations
│   ├── RagService.java              # Embedding + similarity search
│   ├── SchedulingAiService.java     # AI-driven event creation
│   ├── BriefingService.java         # Daily + weekly summaries
│   └── ContextSnapshotService.java  # Maintains AI context document
│
├── ai/
│   ├── AiProvider.java              # Interface
│   ├── OllamaAiProvider.java
│   ├── OpenAiProvider.java
│   ├── AnthropicAiProvider.java
│   └── AiProviderFactory.java
│
├── controller/
│   ├── NoteController.java
│   ├── IdeaController.java
│   ├── TaskController.java
│   ├── EventController.java
│   ├── ChatController.java          # SSE streaming endpoint
│   ├── BriefingController.java
│   └── SettingsController.java
│
└── scheduler/
    ├── DailyBriefingJob.java        # @Scheduled — fires at 8 AM
    ├── WeeklySummaryJob.java        # @Scheduled — fires Sunday 6 PM
    └── ContextSnapshotJob.java      # @Scheduled — fires every Sunday
```

---

## 4. AI Provider Abstraction

All AI operations go through the `AiProvider` interface. The `AiProviderFactory` reads the user's saved settings and returns the correct implementation at runtime.

```java
public interface AiProvider {
    String chat(String systemPrompt, String userMessage);
    String chatStream(String systemPrompt, String userMessage, Consumer<String> tokenCallback);
    List<Double> embed(String text);
    String getProviderName();
}
```

Provider selection logic:

```
UserSettings.aiProvider = "ollama" | "openai" | "anthropic"
       ↓
AiProviderFactory.getProvider(name)
       ↓
Returns the matching @Component bean
```

Spring AI's `ChatClient` and `EmbeddingModel` are configured as conditional beans:

```java
@Bean
@ConditionalOnProperty(name = "aria.ai.provider", havingValue = "ollama")
public ChatClient ollamaChatClient(OllamaChatModel model) { ... }

@Bean
@ConditionalOnProperty(name = "aria.ai.provider", havingValue = "openai")
public ChatClient openAiChatClient(OpenAiChatModel model) { ... }
```

---

## 5. RAG Pipeline

The RAG pipeline enables the AI to answer questions grounded in the user's actual notes.

### Indexing (on note save)
```
User saves/edits a Note
        ↓
NoteService.save() calls RagService.indexNote(note)
        ↓
RagService splits note into chunks (512 tokens, 50-token overlap)
        ↓
Each chunk is embedded via AiProvider.embed()
        ↓
Stored in pgvector as a Document with metadata:
  { noteId, chunkIndex, title, tags }
```

### Retrieval (on chat message)
```
User sends a chat message
        ↓
ChatController receives message
        ↓
AiService.chat(userId, message):
  1. Embed the user's message
  2. VectorStore.similaritySearch(embedding, topK=5)
  3. Retrieve the top-5 matching note chunks
  4. Build system prompt:
       "[CONTEXT SNAPSHOT]\n{contextSnapshot}\n\n
        [RELEVANT NOTES]\n{chunk1}\n---\n{chunk2}\n...\n\n
        Answer the user's question using ONLY the above context.
        Cite which note each fact comes from."
  5. Stream response via AiProvider.chatStream()
        ↓
SSE stream sent to React frontend
```

### Re-embedding on provider switch
When the user switches AI providers (and therefore embedding models), vectors become incompatible. The system handles this as follows:

1. On provider change, `ContextSnapshotService.generateSnapshot()` is called first
2. User is warned: "Switching providers requires re-embedding all your notes. This may take a few minutes."
3. On confirm, a background job deletes all existing vectors and re-embeds all notes with the new provider's embedding model
4. Progress is streamed back to the settings page via SSE

---

## 6. AI Scheduling Pipeline

The AI scheduling feature parses natural language prompts and converts them into calendar event previews.

### Prompt-driven flow
```
User types: "schedule 2 hours for studying tomorrow afternoon"
        ↓
POST /api/chat/schedule { "prompt": "..." }
        ↓
SchedulingAiService.parseScheduleIntent(prompt):
  System prompt instructs the model to respond in JSON:
  {
    "title": "Study session",
    "start_time": "2025-01-15T14:00:00",
    "end_time": "2025-01-15T16:00:00",
    "description": "2-hour focused study block",
    "confidence": 0.92
  }
        ↓
Response returned as EventPreviewDTO to frontend
        ↓
Frontend renders editable preview card
        ↓
User edits if needed → clicks Confirm
        ↓
POST /api/events { source: "AI_SUGGESTED", ... }
        ↓
Event saved to DB
```

### Proactive suggestion flow
```
DailyBriefingJob fires at 8 AM
        ↓
Fetches tasks with due_date <= today + 2 days that have no linked event
        ↓
For each unscheduled near-due task:
  SchedulingAiService.suggestTimeSlot(task, existingEvents)
  → Finds a free slot in the user's calendar
  → Returns a suggestion (not yet saved)
        ↓
Suggestions included in briefing payload as EventPreviewDTO[]
        ↓
User sees suggestion on dashboard, can confirm or dismiss
```

---

## 7. Context Snapshot System

The context snapshot is a special document that captures the user's patterns, preferences, and active context. It is injected into every AI system prompt, ensuring continuity across provider switches.

### Structure of the context snapshot document

```
## About me
[AI-generated paragraph about the user's observed working style]

## Active projects
[List of projects/topics referenced frequently in recent notes]

## Recurring patterns
[Observed habits, e.g. "Usually works on X on weekdays", "Often references Y topic"]

## Recent focus areas
[Topics from notes in the last 30 days]

## My preferences
[User-editable section: preferred tone, depth of AI responses, etc.]
```

### Update schedule
- Auto-updated every Sunday by `ContextSnapshotJob`
- Can be manually regenerated from Settings → AI → "Regenerate context snapshot"
- The "My preferences" section is user-editable and is never overwritten by the auto-update

### Injection into prompts
Every AI call prepends the context snapshot to the system prompt:
```java
String fullSystemPrompt = """
    [USER CONTEXT]
    %s
    
    [TASK]
    %s
    """.formatted(contextSnapshot.getContent(), taskPrompt);
```

---

## 8. Notification System

Notifications are delivered as in-app banners (Tauri system notifications in a future iteration).

| Trigger | Mechanism | Content |
|---|---|---|
| Event reminder | `@Scheduled` polling every minute, checks `reminder_minutes` | "Reminder: {event title} in {N} minutes" |
| Daily briefing | `@Scheduled` cron `0 0 8 * * *` | Today's events + tasks + AI insight |
| Weekly summary | `@Scheduled` cron `0 0 18 * * SUN` | Week in review + next week preview |
| AI scheduling suggestion | Triggered inside daily briefing job | "I noticed '{task}' is due soon — want me to schedule time for it?" |

Notification delivery: Spring Boot pushes updates to the frontend via a `/api/notifications/stream` SSE endpoint. The React frontend subscribes on load and renders banners.

---

## 9. Security Model

For v1 (local single user), security is intentionally lightweight:

- Spring Security is configured but JWT is minimal (single hardcoded user or simple registration)
- JWT token stored in `localStorage` on the frontend
- CORS restricted to `localhost` origins only
- API key for OpenAI/Anthropic stored in `user_settings` table (local DB only, never leaves the machine)
- No HTTPS required for localhost-only communication

For a future public release: proper user registration, bcrypt passwords, refresh tokens, HTTPS enforcement.

---

## 10. Tauri Sidecar Configuration

The Spring Boot JAR runs as a Tauri sidecar — Tauri spawns it as a child process on app launch and terminates it on app close.

```json
// tauri.conf.json
{
  "bundle": {
    "resources": ["backend/aria-backend.jar"],
    "externalBin": ["backend/aria-backend"]
  },
  "plugins": {
    "shell": {
      "sidecar": true
    }
  }
}
```

```rust
// main.rs — spawn sidecar on app start
tauri::Builder::default()
    .setup(|app| {
        let sidecar = app.shell().sidecar("aria-backend")?;
        sidecar.spawn()?;
        Ok(())
    })
```

Startup sequence:
1. Tauri opens native window, loads React frontend
2. React shows a "Starting up..." splash for 2 seconds while Spring Boot initialises
3. React polls `GET /api/health` until 200 OK, then hides splash

---

## 11. Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.9'
services:
  postgres:
    image: pgvector/pgvector:pg16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: aria
      POSTGRES_USER: aria
      POSTGRES_PASSWORD: aria_local
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

---

## 12. Configuration Files

```properties
# application.properties (committed to Git — no secrets)
spring.datasource.url=jdbc:postgresql://localhost:5432/aria
spring.datasource.username=aria
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
aria.briefing.cron=0 0 8 * * *
aria.weekly-summary.cron=0 0 18 * * SUN

# application-local.properties (gitignored — secrets go here)
spring.datasource.password=aria_local
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.embedding.model=nomic-embed-text
# spring.ai.openai.api-key=sk-...
# spring.ai.anthropic.api-key=...
```
