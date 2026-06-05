# Implementation Plan: Vega — Minimalist Personal Task Manager

**Branch**: `001-vega-core` | **Date**: 2026-06-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-vega-core/spec.md`

---

## Summary

Vega is a native Android mobile app designed to help users decide what to do next by providing a focused task management experience. The app operates entirely offline with local Room database storage. Core features include four fixed workflow views (Inbox, Today, Upcoming, Done), natural language task capture, swipe-based interactions, and an intelligent Next Best Action recommendation card.

**Technical approach**: Native Kotlin + Android framework, following MVVM + Repository pattern with Material Design 2 (MDC-Android 1.x) for UI. All data persists locally; no network dependency. Architecture emphasizes separation of concerns: Views handle only UI rendering, ViewModels manage state transformations, Repositories abstract data access, and Room handles persistence.

---

## Technical Context

**Language/Version**: Kotlin (latest stable), targeting Java 11 compatibility

**Primary Dependencies**:
- Android Framework (Jetpack libraries): Room, ViewModel, LiveData/StateFlow, Navigation Component, WorkManager, Hilt
- Material Components for Android (MDC-Android) 1.x — Material Design 2 widgets only, no Material 3
- Kotlin Coroutines for async operations
- JUnit 4 + Mockito for unit testing

**Storage**: Room Database (SQLite) — single `tasks` table, local-only, no migrations required for V1

**Testing**: Unit tests mandatory (parser, ViewModels, scoring logic, repository); UI tests deferred to V2

**Target Platform**: Android 8.0 (API 26) minimum SDK → Android 14 (API 34) target/compile SDK

**Project Type**: Native Android mobile application (single-activity architecture)

**Performance Goals**:
- App launch and interactivity: <1 second on mid-range device
- Swipe gesture response: <200ms
- Natural language parser: <100ms on typical input
- Search results: <500ms for 1000+ tasks

**Constraints**:
- Zero network dependency — fully functional offline
- No third-party NLP/analytics libraries
- No user accounts or remote sync in V1
- Flat data model (no relational complexity)

**Scale/Scope**: Single Activity with 4 Fragment-based screens (Inbox, Today, Upcoming, Done) + Quick Add bottom sheet + Details sheet

---

## Constitution Check

**GATE: Validation against core principles (MUST PASS before Phase 0)**

| Principle | Requirement | Status | Justification |
|-----------|------------|--------|--------------|
| 1. Core Philosophy | Help users decide "what to do next" | ✅ PASS | Next Best Action card + Today focus view directly serve this |
| 2.1 Friction <3s | Task capture in <3 seconds | ✅ PASS | Single text field, no forms, optimistic insert |
| 2.2 Sacred Main Screen | Entire app navigable from Today | ✅ PASS | Bottom nav accessible from every screen, one obvious path |
| 2.3 Today Intelligence | Today view always answers "what next?" | ✅ PASS | Today is default landing, Next Best Action surfaces top task |
| 2.4 Cognitive Load | Cap Today to 3–7 tasks | ✅ PASS | Soft cap enforced with overflow indicator |
| 3.1 5-Field Task Model | title, due_date, priority, state, notes | ✅ PASS | Room schema enforces exactly 5 fields |
| 3.2 Fixed Workflow | Inbox → Today → Upcoming → Done (not customizable) | ✅ PASS | Navigation Component locked to 4 destinations, no custom filters |
| 3.3 Quick Add | Natural language parsing for title/date/priority | ✅ PASS | Custom Kotlin parser with 20+ test cases |
| 3.4 Next Best Action | Weighted scoring by overdue/priority/date/time | ✅ PASS | Pure Kotlin scoring function in TodayViewModel |
| 4. Never Build | No recurring, tags, labels, collaboration, calendar sync, custom views | ✅ PASS | All explicitly excluded from schema and navigation |
| 5.1 Performance | <1s launch, <200ms swipe | ✅ PASS | Room local queries fast, ItemTouchHelper gesture response immediate |
| 5.2 Offline First | Fully functional without network | ✅ PASS | Room only, no remote API, no analytics SDK |
| 5.3 Data Simplicity | Flat schema, no relational complexity | ✅ PASS | Single table, no foreign keys or joins |
| 5.4 No Bloat | Minimize dependencies, no analytics/push SDKs | ✅ PASS | Only Jetpack + MDC-Android, NotificationCompat is built-in |
| 6. Code Quality | Every core action unit tested, NLP parser 20+ cases | ✅ PASS | Test suite design section below specifies test coverage |
| 7. Decision Hierarchy | Decisions favor friction reduction and user clarity | ✅ PASS | Architecture choices (MVVM, Repository) support these values |

**Result**: ✅ ALL GATES PASSED — No constraint violations, no exemptions needed

---

## Project Structure

### Documentation (this feature)

```text
specs/001-vega-core/
├── spec.md              # Feature specification (user scenarios, requirements, success criteria)
├── plan.md              # This file (technical context, architecture, design artifacts)
├── research.md          # Phase 0 research findings (included in plan below)
├── data-model.md        # Phase 1 design (entity definitions, relationships)
├── quickstart.md        # Phase 1 design (validation scenarios, end-to-end workflows)
├── contracts/           # Phase 1 design (external interfaces — N/A for this mobile app)
├── checklists/
│   └── requirements.md  # Quality validation checklist
└── tasks.md             # Phase 2 output (generated by /speckit.tasks command)
```

### Source Code (repository root)

```text
vega/                                    # Project root
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vega/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── activities/
│   │   │   │   │   │   └── MainActivity.kt
│   │   │   │   │   ├── fragments/
│   │   │   │   │   │   ├── InboxFragment.kt
│   │   │   │   │   │   ├── TodayFragment.kt
│   │   │   │   │   │   ├── UpcomingFragment.kt
│   │   │   │   │   │   ├── DoneFragment.kt
│   │   │   │   │   │   ├── QuickAddBottomSheetFragment.kt
│   │   │   │   │   │   └── TaskDetailFragment.kt
│   │   │   │   │   ├── adapters/
│   │   │   │   │   │   └── TaskListAdapter.kt
│   │   │   │   │   ├── viewmodels/
│   │   │   │   │   │   ├── InboxViewModel.kt
│   │   │   │   │   │   ├── TodayViewModel.kt
│   │   │   │   │   │   ├── UpcomingViewModel.kt
│   │   │   │   │   │   ├── DoneViewModel.kt
│   │   │   │   │   │   └── QuickAddViewModel.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       └── ThemeManager.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── VegaDatabase.kt
│   │   │   │   │   │   ├── TaskDao.kt
│   │   │   │   │   │   └── Task.kt (Room entity)
│   │   │   │   │   └── repository/
│   │   │   │   │       └── TaskRepository.kt
│   │   │   │   ├── parser/
│   │   │   │   │   ├── NaturalLanguageParser.kt
│   │   │   │   │   └── ParseResult.kt
│   │   │   │   ├── workers/
│   │   │   │   │   └── EndOfDaySnoozeWorker.kt
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt (Hilt configuration)
│   │   │   │   └── VegaApplication.kt
│   │   │   ├── res/
│   │   │   │   ├── values/themes.xml, colors.xml, strings.xml, dimens.xml
│   │   │   │   ├── values-night/colors.xml (dark theme)
│   │   │   │   ├── layout/ (all XML layouts)
│   │   │   │   ├── menu/bottom_nav_menu.xml
│   │   │   │   ├── navigation/nav_graph.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── java/com/vega/
│   │   │       ├── parser/NaturalLanguageParserTest.kt (20+ test cases)
│   │   │       ├── ui/viewmodels/ (ViewModel state tests)
│   │   │       ├── ui/logic/ (NextBestActionScoringTest)
│   │   │       └── data/repository/ (TaskRepositoryTest with in-memory DB)
│   │   └── androidTest/ (deferred to V2)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts (project-level)
├── settings.gradle.kts
└── gradle.properties
```

**Structure Decision**: Single-activity MVVM architecture with 4 Fragment-based screens. All business logic isolated in ViewModels. Room database abstracted via Repository. Natural language parser is a standalone module with comprehensive test suite. This structure supports the constitution's requirement for clean separation of concerns and testability.

---

## Phase 0: Research & Clarifications

**Status**: All technical decisions provided in planning input — no external research required.

### Technical Decisions Ratified

| Area | Decision | Rationale |
|------|----------|-----------|
| **Language** | Kotlin on Android | Primary market for task managers; native performance meets <1s launch requirement |
| **UI Framework** | MDC-Android 1.x (Material Design 2) | Constitution specifies Material Design 2; MDC-Android is canonical implementation |
| **Architecture** | MVVM + Repository Pattern | Clean separation required by constitution section 6; enables testability of all core actions |
| **Database** | Room (SQLite) | Local-only, zero network dependency satisfies offline-first principle; Room generates schema from Kotlin entities |
| **Dependency Injection** | Hilt | Standard Jetpack approach; minimal boilerplate; integrates cleanly with ViewModel injection |
| **Navigation** | Jetpack Navigation Component | Handles bottom nav binding automatically; supports Fragment transitions; standard in modern Android apps |
| **Natural Language Parser** | Custom Kotlin implementation | No third-party NLP library (constitution principle 5.4); lightweight, tailored to task capture use case |
| **Swipe Gestures** | ItemTouchHelper + RecyclerView | Standard Android pattern; provides Material-appropriate visual feedback; reduces code complexity |
| **Async Operations** | Kotlin Coroutines | Native Kotlin feature; integrates with ViewModel and Room; replaces callback hell |
| **Notifications** | NotificationCompat + WorkManager | Built-in to Jetpack; supports scheduling (end-of-day snooze); no external SDK required |
| **Testing** | JUnit 4 + Mockito + in-memory Room | Standard Android test stack; in-memory DB enables fast repository tests without mocking |

---

## Phase 1: Design & Contracts

### 1.1 Data Model

**Task Entity** (Room)

```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDate: Long? = null,                    // Epoch milliseconds
    val priority: TaskPriority = TaskPriority.NONE,
    val state: TaskState,                         // INBOX, TODAY, UPCOMING, DONE
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
enum class TaskState { INBOX, TODAY, UPCOMING, DONE }
```

### 1.2 Next Best Action Scoring

```kotlin
fun scoreTask(task: Task, now: Long): Int {
    var score = 0
    if (task.dueDate != null && task.dueDate < now) score += 100   // Overdue
    score += when (task.priority) {                                 // Priority
        TaskPriority.HIGH -> 30
        TaskPriority.MEDIUM -> 15
        TaskPriority.LOW -> 5
        TaskPriority.NONE -> 0
    }
    if (task.dueDate != null && (task.dueDate - now) <= 2.hours) score += 40  // Due soon
    if (task.dueDate != null && isSameDay(task.dueDate, now)) score += 20     // Due today
    return score
}
```

### 1.3 Natural Language Parser

**Input**: "Buy milk tomorrow 10am high"  
**Output**: ParseResult(title="Buy milk", dueDate=tomorrow 10am, priority=HIGH)

**Test Cases**: Minimum 20 covering all scenarios (priority extraction, date/time parsing, title cleanup, ambiguous inputs, edge cases)

### 1.4 Swipe Gestures (ItemTouchHelper)

| Direction | Action |
|-----------|--------|
| Right | Mark Done (state → DONE) |
| Left | Reveal: Postpone, Delete |
| Long Press | Open detail/edit sheet |

### 1.5 End-of-Day Snooze (WorkManager + Notifications)

- Trigger: 21:00 daily
- Action: Sequential bottom sheets for each unfinished Today task
- Options: Move tomorrow, Keep in Today, Mark done, Delete

### 1.6 Notification System

- Channel: `vega_reminders` (importance HIGH)
- Types: End-of-day snooze nudge, due-time reminders
- User Control: Toggle in Settings

---

## Phase 1: Validation Scenarios

**Scenario 1**: User captures "Buy milk tomorrow 10am high" → Task created with all fields populated

**Scenario 2**: Today has 8 tasks → Shows 7 by default, "+1 more" indicator when trying to add 8th

**Scenario 3**: Today has 3 tasks (overdue, due in 30min, due tomorrow) → Next Best Action shows overdue task first

**Scenario 4**: User swipes right on task → Marks done in <200ms, moves to Done view

**Scenario 5**: Unfinished tasks at 21:00 → End-of-day snooze prompt appears with 4 options per task

**Scenario 6**: App in airplane mode → All operations succeed, data persists locally

---

## Agent Context Update

**Action**: Update `.github/copilot-instructions.md` to point to this plan:

```markdown
<!-- SPECKIT START -->
Current plan: specs/001-vega-core/plan.md
Feature spec: specs/001-vega-core/spec.md
Constitution: .specify/memory/constitution.md
<!-- SPECKIT END -->
```

---

## Constitution Check (Post-Design)

✅ **ALL GATES STILL PASSING** — Design artifacts remain aligned with all constitution principles. Ready for implementation.

---

## Complexity Tracking

| Area | Justification |
|------|--------------|
| **MVVM + Repository** | Required by constitution section 6 for testability and separation of concerns |
| **Natural Language Parser** | Constitution 5.4 forbids third-party NLP; custom lightweight parser tailored to task capture |
| **End-of-Day Snooze** | Serves constitution 2.4 cognitive load principle; sequential prompts prevent overwhelm |
| **Swipe Gestures** | Standard Android pattern; reduces friction per constitution 2.1 |

---

**Status**: ✅ Phase 1 design complete

**Next Step**: Run `/speckit.tasks` to generate implementation tasks

**Version**: Plan 1.0.0 | **Created**: 2026-06-04
