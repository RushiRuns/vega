# Vega - Minimalist Personal Task Manager

Vega is a minimalist Android task manager designed to help you decide what to do next, not just track everything. It enforces a cognitive load cap, incorporates intelligent recommendations, supports swift natural language task capture, and guides users through a structured daily reset.

---

## 🎯 Philosophy

Vega is built on three core design principles:
1. **Natural Language Capture**: Add tasks in under 3 seconds using intuitive freeform text.
2. **Fixed Workflow**: Inbox ➔ Today ➔ Upcoming ➔ Done (no custom lists, folders, or complex tagging).
3. **Next Best Action**: Displays a single, highly recommended task at the top of the Today view so you always know what to work on next.

---

## ✨ Features

- 📝 **Natural Language Task Parsing**: Automatically extracts titles, due dates, times, and priorities from text inputs.
- 🎯 **Cognitive Focus Cap**: Only 7 tasks are visible in the Today view by default (with a collapsible `+N more` overflow indicator) to prevent overwhelm.
- 🌙 **End-of-Day Snooze Ritual**: Displays a sequential bottom sheet at 21:00 for all unfinished Today tasks to move them to tomorrow, keep them, mark done, or delete.
- 📴 **Offline-First & Local-First**: Zero server sync, 100% database persistence on device.
- 🎨 **Theme Support**: Full DayNight styling support (System, Light, Dark) with dynamic runtime activity recreation.
- ⚡ **Highly Responsive Swipes**: Swipe right to instantly mark a task done; swipe left to reveal postpone and delete overlays.

---

## 🏗️ Architecture & Tech Stack

Vega is built on modern Android architecture patterns:
- **Pattern**: MVVM (Model-View-ViewModel) + Clean Data Layer.
- **Dependency Injection**: Hilt (Dagger) for database, repository, and parser providers.
- **Local Persistence**: Room Database (with custom performance indices on `state`, `dueDate`, `createdAt`, and `updatedAt`).
- **Reactive UI**: StateFlow and SharedFlow via Kotlin Coroutines.
- **View Layer**: ViewBinding with Single-Activity Navigation Graph.
- **Background Tasks**: WorkManager scheduling daily reminders (18:00), weekly reviews (Sunday 18:00), and snooze rituals (21:00).

---

## 📊 Database Schema & Indexes

### Entity: `Task` (Table: `tasks`)

| Column Name | Type | Key | Description |
|---|---|---|---|
| `id` | `TEXT` | Primary Key | UUID generated locally |
| `title` | `TEXT` | - | Task title |
| `dueDate` | `INTEGER` | Index | Optional timestamp in milliseconds |
| `priority` | `TEXT` | - | Enum: `NONE`, `LOW`, `MEDIUM`, `HIGH` |
| `state` | `TEXT` | Index | Enum: `INBOX`, `TODAY`, `UPCOMING`, `DONE` |
| `notes` | `TEXT` | - | Optional text notes |
| `createdAt` | `INTEGER` | Index | Task creation audit timestamp |
| `updatedAt` | `INTEGER` | Index | Last modified audit timestamp |

*Database Version: `2` (supporting destructive migration fallbacks).*

---

## 📝 Natural Language Parser Rules

The natural language engine parses text in real-time. It strips extracted tokens from the input to form the final title, and removes any trailing prepositions left over.

### 1. Priority Keywords
- **HIGH**: `high`, `urgent`, `asap`, `critical`, `urgent:`
- **MEDIUM**: `medium`, `normal`
- **LOW**: `low`, `whenever`
- **NONE**: Default state if no keyword matches.

### 2. Date/Time Keywords
- **MM/DD/YYYY**: Explicit date matching (e.g. `12/25/2026`).
- **Relative Keywords**: `today`, `tomorrow`.
- **Weekday Matching**: `next monday`, `next friday`, etc. (advances relative to the base current time).
- **In N Days**: `in 5 days`, `in 1 day`.
- **Time Formats**: `3pm`, `10:30`, `3:30am`, `15:00`. If no time is specified, it defaults to `00:00:00.000`.

### 3. Trailing Preposition Stripping
To avoid ugly titles like *"Buy milk at"* when parsing *"Buy milk at 3pm"*, the parser strips trailing prepositions matching `at`, `by`, `on`, `for`, `to`, and `in` at the end of the text.

---

## 🧠 Next Best Action Scoring Weights

The Today view recommendations algorithm scores and ranks tasks to recommend what to do next.

### Scoring Criteria

| Condition | Points Added |
|---|---|
| Overdue (`dueDate` < current time) | **+100** |
| Due within 2 hours | **+40** |
| Due today | **+20** |
| High Priority (`HIGH`) | **+30** |
| Medium Priority (`MEDIUM`) | **+15** |
| Low Priority (`LOW`) | **+5** |
| No Priority (`NONE`) | **0** |

### Tie-Breaking Hierarchy
If two tasks calculate to the same score, ties are resolved deterministically using:
1. **Due Date** (earlier dates first).
2. **Priority Ordinal** (highest priority first).
3. **Alphabetical Title** (A-Z).
4. **Task ID** (UUID alphabetical tie-breaker).

---

## 🚀 Getting Started

### 1. Build the project
```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

### 2. Run unit tests
```bash
# Run Room, ViewModels, Worker preferences, and NL parser test suites
./gradlew testDebugUnitTest
```

### 3. Install on connected emulator or device
```bash
./gradlew installDebug
```

---

## ⚙️ Configuration & Preferences

All settings and toggles are stored in `SharedPreferences` under the file name `"vega_prefs"`:
- `pref_theme`: Theme override (`"system"`, `"light"`, `"dark"`).
- `pref_end_of_day_reminders`: End-of-day snooze ritual switch (`true`/`false`).
- `pref_daily_reminders`: 18:00 daily reminder switch (`true`/`false`).
- `pref_weekly_review`: Sunday 18:00 weekly review prompt switch (`true`/`false`).
