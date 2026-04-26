# Frontend Architecture
**Project:** ARIA
**Version:** 1.0.0
**Stack:** React 18 + Vite + Tailwind CSS + Zustand + Tauri

---

## 1. Folder Structure

```
src/
├── api/                        # All Axios calls — one file per backend module
│   ├── client.ts               # Axios instance with auth interceptor
│   ├── notes.api.ts
│   ├── ideas.api.ts
│   ├── tasks.api.ts
│   ├── events.api.ts
│   ├── chat.api.ts
│   ├── briefing.api.ts
│   ├── settings.api.ts
│   └── context.api.ts
│
├── components/                 # Reusable UI components (no business logic)
│   ├── ui/                     # Primitives: Button, Input, Badge, Modal, Card
│   ├── notes/
│   │   ├── NoteCard.tsx
│   │   ├── NoteEditor.tsx
│   │   └── NoteList.tsx
│   ├── tasks/
│   │   ├── TaskItem.tsx
│   │   └── TaskList.tsx
│   ├── events/
│   │   ├── EventPreviewCard.tsx    # AI scheduling suggestion preview
│   │   └── CalendarView.tsx
│   ├── ideas/
│   │   ├── IdeaFeed.tsx
│   │   └── QuickCaptureBar.tsx
│   ├── chat/
│   │   ├── ChatPanel.tsx
│   │   ├── ChatMessage.tsx
│   │   └── SourceChip.tsx          # Shows which note the AI cited
│   ├── briefing/
│   │   ├── DailyBriefingBanner.tsx
│   │   └── WeeklySummaryModal.tsx
│   ├── notifications/
│   │   └── NotificationToast.tsx
│   └── layout/
│       ├── Sidebar.tsx
│       ├── TopBar.tsx
│       └── AppShell.tsx
│
├── pages/                      # Route-level components
│   ├── DashboardPage.tsx       # Main landing view
│   ├── NotesPage.tsx
│   ├── CalendarPage.tsx
│   ├── TasksPage.tsx
│   ├── IdeasPage.tsx
│   ├── ChatPage.tsx
│   ├── SettingsPage.tsx
│   └── OnboardingPage.tsx
│
├── store/                      # Zustand global state
│   ├── useNotesStore.ts
│   ├── useTasksStore.ts
│   ├── useEventsStore.ts
│   ├── useIdeasStore.ts
│   ├── useChatStore.ts
│   ├── useSettingsStore.ts
│   └── useNotificationsStore.ts
│
├── hooks/                      # Reusable custom hooks
│   ├── useSSE.ts               # Generic SSE hook (used for chat stream, notifications)
│   ├── useDebounce.ts          # For note autosave
│   └── useLocalStorage.ts      # For JWT token storage
│
├── types/                      # TypeScript interfaces matching API DTOs
│   ├── note.types.ts
│   ├── task.types.ts
│   ├── event.types.ts
│   ├── idea.types.ts
│   ├── chat.types.ts
│   └── settings.types.ts
│
├── utils/
│   ├── date.utils.ts           # Format helpers using date-fns
│   └── token.utils.ts          # JWT decode, expiry check
│
├── App.tsx                     # Router + auth guard
└── main.tsx                    # Entry point
```

---

## 2. Routing

```tsx
// App.tsx
<Routes>
  <Route path="/onboarding" element={<OnboardingPage />} />
  <Route element={<AuthGuard />}>
    <Route element={<AppShell />}>
      <Route path="/"         element={<DashboardPage />} />
      <Route path="/notes"    element={<NotesPage />} />
      <Route path="/calendar" element={<CalendarPage />} />
      <Route path="/tasks"    element={<TasksPage />} />
      <Route path="/ideas"    element={<IdeasPage />} />
      <Route path="/chat"     element={<ChatPage />} />
      <Route path="/settings" element={<SettingsPage />} />
    </Route>
  </Route>
</Routes>
```

`AuthGuard` checks for a valid JWT in localStorage. If missing or expired, redirects to a local login screen.

On first launch, `onboardingCompleted = false` in settings → redirect to `/onboarding`.

---

## 3. Zustand Store Patterns

Each store follows the same pattern: local state + async actions that call the API layer.

```ts
// store/useNotesStore.ts
interface NotesStore {
  notes: Note[];
  isLoading: boolean;
  selectedNoteId: string | null;
  fetchNotes: (params?: NoteQueryParams) => Promise<void>;
  createNote: (data: CreateNoteRequest) => Promise<Note>;
  updateNote: (id: string, data: Partial<Note>) => Promise<void>;
  deleteNote: (id: string) => Promise<void>;
  setSelectedNote: (id: string | null) => void;
}

const useNotesStore = create<NotesStore>((set, get) => ({
  notes: [],
  isLoading: false,
  selectedNoteId: null,

  fetchNotes: async (params) => {
    set({ isLoading: true });
    const data = await notesApi.getAll(params);
    set({ notes: data.content, isLoading: false });
  },

  createNote: async (data) => {
    const note = await notesApi.create(data);
    set(state => ({ notes: [note, ...state.notes] }));
    return note;
  },

  updateNote: async (id, data) => {
    const updated = await notesApi.patch(id, data);
    set(state => ({
      notes: state.notes.map(n => n.id === id ? updated : n)
    }));
  },

  deleteNote: async (id) => {
    await notesApi.delete(id);
    set(state => ({
      notes: state.notes.filter(n => n.id !== id),
      selectedNoteId: state.selectedNoteId === id ? null : state.selectedNoteId
    }));
  },

  setSelectedNote: (id) => set({ selectedNoteId: id })
}));
```

---

## 4. SSE (Streaming) Pattern

Two SSE connections are maintained:

### Chat streaming
```ts
// hooks/useSSE.ts — generic SSE hook
function useSSE(url: string, onToken: (t: string) => void, onDone: () => void) {
  useEffect(() => {
    const es = new EventSource(url, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    es.addEventListener('token', e => onToken(e.data));
    es.addEventListener('done', () => { onDone(); es.close(); });
    es.onerror = () => es.close();
    return () => es.close();
  }, [url]);
}
```

Usage in `ChatPanel.tsx`:
```tsx
const [reply, setReply] = useState('');
const [streaming, setStreaming] = useState(false);

const sendMessage = (msg: string) => {
  setReply('');
  setStreaming(true);
  // SSE connection is opened, tokens appended to reply
  openChatStream(msg, token => setReply(r => r + token), () => setStreaming(false));
};
```

### Notification stream
A persistent SSE connection in `App.tsx` that routes events to `useNotificationsStore`:
```ts
// Opened once on login, closed on logout
const es = new EventSource('/api/notifications/stream');
es.addEventListener('reminder', e => notifStore.addReminder(JSON.parse(e.data)));
es.addEventListener('briefing_ready', () => notifStore.markBriefingReady());
es.addEventListener('ai_suggestion', e => notifStore.addSuggestion(JSON.parse(e.data)));
```

---

## 5. Autosave Pattern (Notes)

Notes autosave 1 second after the user stops typing.

```ts
// hooks/useDebounce.ts
function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

// NoteEditor.tsx
const [body, setBody] = useState(note.body);
const debouncedBody = useDebounce(body, 1000);

useEffect(() => {
  if (debouncedBody !== note.body) {
    updateNote(note.id, { body: debouncedBody });
  }
}, [debouncedBody]);
```

A subtle "Saving..." → "Saved" indicator in the top-right of the editor provides feedback.

---

## 6. Page Layouts

### DashboardPage
```
┌──────────────────────────────────────────────────────────────┐
│ [Daily Briefing Banner — dismissable, top of page]           │
├───────────────────────┬──────────────────────────────────────┤
│ Quick Capture Bar     │                                      │
│ ─────────────────     │   Calendar (weekly mini view)        │
│ Pinned Notes (2-3)    │                                      │
│ ─────────────────     ├──────────────────────────────────────┤
│ Tasks due today       │   AI Scheduling Suggestions          │
│ ─────────────────     │   (if any, dismissable cards)        │
│ Recent Ideas          │                                      │
└───────────────────────┴──────────────────────────────────────┘
```

### NotesPage
```
┌───────────────┬──────────────────────────────────────────────┐
│ Note list     │                                              │
│ ─────────     │   NoteEditor                                │
│ Search bar    │   (title + body, autosave indicator)        │
│ Filter by tag │                                              │
│ ─────────     │   Tags row                                  │
│ [Note cards]  │   ─────────────────────────────────────────│
│               │   Ask AI about this note [button]           │
└───────────────┴──────────────────────────────────────────────┘
```

### CalendarPage
```
┌──────────────────────────────────────────────────────────────┐
│ [Month / Week toggle]    [+ New Event button]                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   react-big-calendar — full page                             │
│   AI-suggested events shown in different color               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### ChatPage
```
┌──────────────────────────────────────────────────────────────┐
│   Message history (scrollable)                               │
│   ─────────────────────────────────────────────────────────  │
│   AI message with source chips: [Meeting notes] [Sprint 4]  │
│                                                              │
│   ─────────────────────────────────────────────────────────  │
│   [  Type a message or ask to schedule something...      ] ▶ │
└──────────────────────────────────────────────────────────────┘
```

---

## 7. Onboarding Flow

3-step wizard rendered at `/onboarding`:

```
Step 1: Check Ollama
  ┌────────────────────────────────────┐
  │ ✓ Ollama detected on port 11434    │
  │   or                               │
  │ ✗ Ollama not found                 │
  │   → Download Ollama [link]         │
  └────────────────────────────────────┘

Step 2: Pull a model
  ┌────────────────────────────────────┐
  │ Select model: [llama3.2 ▼]        │
  │ [Pull model]                       │
  │ ████████░░░░░░░░ 52% — 2.1 GB     │
  └────────────────────────────────────┘

Step 3: Quick tour
  ┌────────────────────────────────────┐
  │ Here's what you can do:            │
  │ → Notes: capture anything          │
  │ → Calendar: manage your time       │
  │ → Chat: ask your AI assistant      │
  │ [Get started →]                    │
  └────────────────────────────────────┘
```

---

## 8. TypeScript Types

```ts
// types/note.types.ts
export interface Note {
  id: string;
  title?: string;
  body: string;
  tags: string[];
  isPinned: boolean;
  createdAt: string;
  updatedAt: string;
}

// types/event.types.ts
export type EventSource = 'MANUAL' | 'AI_SUGGESTED';

export interface CalendarEvent {
  id: string;
  title: string;
  startTime: string;
  endTime?: string;
  description?: string;
  reminderMinutes: number;
  source: EventSource;
}

export interface EventPreview {
  title: string;
  startTime: string;
  endTime: string;
  description?: string;
  confidence: number;
}

// types/chat.types.ts
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: Array<{ noteId: string; noteTitle: string }>;
  isStreaming?: boolean;
}
```

---

## 9. Axios Client Setup

```ts
// api/client.ts
const client = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
});

// Attach JWT to every request
client.interceptors.request.use(config => {
  const token = localStorage.getItem('aria_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Handle 401 globally — redirect to login
client.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('aria_token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);
```

---

## 10. Environment Configuration

```
.env.development
VITE_API_BASE_URL=http://localhost:8080/api

.env.production
VITE_API_BASE_URL=http://localhost:8080/api   # same for desktop build
```

In Tauri production builds, the API URL is always `localhost` since Spring Boot runs as a sidecar on the same machine.
