# ARIA — AI-powered Personal Knowledge & Schedule Manager

> A local-first desktop app that unifies notes, tasks, calendar, and ideas with an AI assistant that knows your full context.

---

## What it does

- **Unified workspace** — notes, tasks, events, and quick ideas in one place
- **AI chat** — ask questions about your notes and get answers grounded in what you've actually written (RAG)
- **AI scheduling** — type "block 2 hours for studying tomorrow afternoon" and it's added to your calendar after your confirmation
- **Daily briefing** — every morning, a smart summary of your day
- **Weekly review** — every Sunday, an AI-narrated recap of your week
- **Local-first** — all data stays on your machine; no cloud required
- **Provider-agnostic** — works with Ollama (free, local), OpenAI, or Anthropic

---

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java | 21+ | [adoptium.net](https://adoptium.net) |
| Node.js | 20+ | [nodejs.org](https://nodejs.org) |
| Rust | stable | [rustup.rs](https://rustup.rs) |
| Docker | any | [docker.com](https://docker.com) |
| Ollama | latest | [ollama.com](https://ollama.com) |

---

## Local Development Setup

### 1. Clone the repo
```bash
git clone https://github.com/yourname/aria.git
cd aria
```

### 2. Start the database
```bash
docker compose up -d
```
This starts PostgreSQL 16 with pgvector on port 5432.

### 3. Configure local secrets
```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```
Edit `application-local.properties` and set your DB password and Ollama model if different from defaults.

### 4. Start Ollama and pull required models
```bash
ollama serve                          # start Ollama if not already running
ollama pull llama3.2                  # chat model
ollama pull nomic-embed-text          # embedding model
```

### 5. Start the backend
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Spring Boot will run at `http://localhost:8080`.

### 6. Start the frontend (dev mode)
```bash
cd frontend
npm install
npm run dev
```
Vite dev server runs at `http://localhost:5173`.

### 7. (Optional) Run as Tauri desktop app
```bash
cd frontend
npm run tauri dev
```

---

## Running Tests

```bash
# Backend unit + integration tests
cd backend
./mvnw test

# Run a specific test class
./mvnw test -Dtest=NoteServiceTest

# Frontend (when tests are added)
cd frontend
npm run test
```

---

## Project Structure

```
aria/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/aria/
│   ├── src/main/resources/
│   │   ├── application.properties        # committed — no secrets
│   │   ├── application-local.properties  # gitignored — secrets here
│   │   └── db/migration/                 # Flyway SQL files
│   └── pom.xml
│
├── frontend/                   # React + Vite + Tauri
│   ├── src/
│   ├── src-tauri/              # Tauri Rust shell
│   └── package.json
│
├── docs/                       # All project documentation
│   ├── PRD.md                  # Product requirements
│   ├── TECHNICAL_DESIGN.md     # Architecture & design decisions
│   ├── DATABASE_SCHEMA.md      # Schema + Flyway migrations
│   ├── API_DESIGN.md           # All REST endpoints
│   └── FRONTEND_ARCHITECTURE.md
│
├── docker-compose.yml
└── README.md
```

---

## Git Workflow

### Branch naming
```
main              → always stable, protected
develop           → integration branch
feature/<name>    → new features (e.g. feature/rag-pipeline)
fix/<name>        → bug fixes
chore/<name>      → config, deps, refactors
```

### Commit message format (Conventional Commits)
```
feat(notes): add autosave with 1-second debounce
fix(auth): correct JWT expiry not being refreshed on re-login
feat(ai): implement RAG pipeline with pgvector similarity search
test(notes): add unit tests for NoteService
docs(readme): update local setup steps
chore(deps): upgrade Spring Boot to 3.3.2
```

### Releasing
```bash
# Tag a release when a phase milestone is complete
git tag -a v0.1.0 -m "Phase 1: Core CRUD backend"
git push origin v0.1.0
```

---

## Environment Variables / Secrets

**Never commit secrets to Git.** All sensitive values go in `application-local.properties` (already in `.gitignore`).

| Variable | Description | Default |
|---|---|---|
| `spring.datasource.password` | PostgreSQL password | `aria_local` |
| `spring.ai.ollama.base-url` | Ollama URL | `http://localhost:11434` |
| `spring.ai.openai.api-key` | OpenAI key (optional) | — |
| `spring.ai.anthropic.api-key` | Anthropic key (optional) | — |

---

## Database Migrations

Schema changes are managed by **Flyway**. Never alter the database manually.

```bash
# Create a new migration
touch backend/src/main/resources/db/migration/V4__your_change_description.sql
```

Migrations run automatically on Spring Boot startup. Naming convention: `V{number}__{description}.sql`.

---

## Building for Distribution

```bash
# 1. Build the Spring Boot JAR
cd backend
./mvnw package -DskipTests
# Output: backend/target/aria-backend.jar

# 2. Build the Tauri installer (bundles JAR + React)
cd frontend
npm run tauri build
# Output: frontend/src-tauri/target/release/bundle/
#   Windows: aria_1.0.0_x64_en-US.msi
#   macOS:   aria_1.0.0_x64.dmg
#   Linux:   aria_1.0.0_amd64.AppImage
```

---

## Documentation Index

| Document | Description |
|---|---|
| [PRD.md](docs/PRD.md) | What the app does and why — all feature requirements |
| [TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md) | Architecture, module breakdown, AI pipelines |
| [DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) | Full schema, Flyway migrations, indexing strategy |
| [API_DESIGN.md](docs/API_DESIGN.md) | Every REST endpoint with request/response shapes |
| [FRONTEND_ARCHITECTURE.md](docs/FRONTEND_ARCHITECTURE.md) | Folder structure, state management, component design |

---

## Roadmap

| Version | Milestone |
|---|---|
| v0.1.0 | Phase 0–1: Repo setup + Core CRUD backend |
| v0.2.0 | Phase 2: React frontend + calendar UI |
| v0.3.0 | Phase 3: AI chat + RAG pipeline + scheduling |
| v1.0.0 | Phase 4: Tauri packaging + onboarding flow |
| v1.x | Cloud sync, mobile app, external calendar integrations |

---

## License

MIT — free to use, modify, and distribute.
