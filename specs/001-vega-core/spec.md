# Feature Specification: Vega — Minimalist Personal Task Manager

**Feature Branch**: `001-vega-core`

**Created**: 2026-06-04

**Status**: Draft

**Input**: User description: "Build Vega, a minimalist personal task manager designed around one goal: helping the user decide what to do next, not just store tasks."

---

## User Scenarios & Testing

### User Story 1 — Quick Task Capture with Natural Language (Priority: P1)

A user is in a busy meeting and needs to capture a task immediately. They tap the floating action button and see a single text input field. They type "Call dentist tomorrow 10am high" and the system instantly captures the task with title (Call dentist), due date (tomorrow at 10am), and priority (high). If any part is ambiguous, the task is captured anyway with unresolved fields blank. This is the most frequent user interaction and must never block or frustrate.

**Why this priority**: Capture friction is the #1 source of task manager abandonment. Any delay or form-filling discourages use. This feature removes the barrier to getting ideas out of your head.

**Independent Test**: Can be fully tested by verifying that a user can capture a task in under 3 seconds without completing a form, with natural language parsing extracting title, date, time, and priority from freeform input.

**Acceptance Scenarios**:
1. **Given** user is on any screen, **When** user taps floating action button and types "Buy milk", **Then** task is instantly created with title "Buy milk" and no other fields set
2. **Given** user in quick add, **When** user types "Finish report by Friday high", **Then** system extracts title (Finish report), due date (Friday), and priority (high)
3. **Given** user in quick add, **When** user types ambiguous input like "Meeting 3pm or 4pm?", **Then** task is captured with title and time left blank, allowing edit later
4. **Given** user in quick add, **When** user completes entry, **Then** input field clears immediately for next capture

---

### User Story 2 — Today View with Focus Cap (Priority: P1)

User opens Vega and lands on Today view showing only 3 to 7 tasks they previously promoted. If they have more tasks in Today, a subtle indicator shows overflow count but tasks are not displayed by default. This prevents task lists from becoming guilt lists and keeps focus on what matters now.

**Why this priority**: Cognitive overload is the second reason task managers fail. A focused list of 3–7 tasks is actionable; 50 tasks is paralyzing. This view is the home screen and sets the tone for daily productivity.

**Independent Test**: Can be tested by verifying that Today view displays max 3–7 tasks by default, shows overflow indicator when exceeded, and user can expand to see hidden tasks on demand.

**Acceptance Scenarios**:
1. **Given** user opens Vega, **When** they have not selected another view, **Then** Today view appears as default landing screen
2. **Given** user has 5 tasks in Today, **When** they view Today, **Then** all 5 are displayed
3. **Given** user has 12 tasks in Today, **When** they view Today, **Then** first 7 are shown, overflow count displayed ("+5 more"), hidden tasks accessible by scrolling
4. **Given** user is in Today view, **When** they have 0 tasks, **Then** message appears: "Nothing in Today. Inbox or Upcoming have tasks to review."

---

### User Story 3 — Next Best Action Card (Priority: P1)

At the top of Today view, a prominent card surfaces one task the user should do right now. The algorithm weighs overdue status (highest weight), priority level, due date proximity, and time of day. The card appears naturally — not labeled as "AI" or "smart" — making the app feel intuitive rather than mechanical.

**Why this priority**: This card directly serves the core mission: help users decide what to do next. It removes the cognitive burden of prioritization in the moment.

**Independent Test**: Can be tested by verifying that one task is always surfaced as Next Best Action, that overdue tasks are prioritized highest, and that dismissing it removes it from the card (without affecting the task).

**Acceptance Scenarios**:
1. **Given** user has 3 tasks in Today (one overdue, two on-time), **When** they view Today, **Then** overdue task appears in Next Best Action card
2. **Given** user has 2 on-time tasks (one due today, one due tomorrow), **When** at 9pm, **Then** task due today appears in Next Best Action card
3. **Given** Next Best Action card is showing a task, **When** user swipes the card left, **Then** it dismisses and does not reappear until user refreshes or manually selects another task
4. **Given** user completes the Next Best Action task, **When** they mark it done, **Then** card refreshes to show next recommended task

---

### User Story 4 — Fixed Workflow (Inbox → Today → Upcoming → Done) (Priority: P1)

All tasks flow through four fixed views in order: Inbox (entry point), Today (focused work list), Upcoming (future tasks), and Done (archive). The user manually moves tasks between views. Views are not customizable; navigation is always via bottom navigation bar with four items.

**Why this priority**: Without a fixed workflow, users become paralyzed by choice. A linear, opinionated flow removes decision fatigue and enforces a healthy task management discipline.

**Independent Test**: Can be tested by verifying that new tasks land in Inbox, user can move tasks to Today, Upcoming tasks respect due date ranges, Done view shows completed tasks, and navigation bar always shows all four views.

**Acceptance Scenarios**:
1. **Given** user creates a new task, **When** task is created, **Then** it lands in Inbox (not Today)
2. **Given** user is in Inbox, **When** they move a task to Today, **Then** task appears in Today and no longer in Inbox
3. **Given** user has tasks with due dates 2 weeks away, **When** they move to Upcoming, **Then** they appear in Upcoming (not Today or Done)
4. **Given** user marks a task done, **When** action completes, **Then** task moves to Done and no longer appears in other views
5. **Given** user is on any screen, **When** they look at bottom navigation, **Then** all four views (Inbox, Today, Upcoming, Done) are always accessible

---

### User Story 5 — End-of-Day Snooze Ritual (Priority: P2)

When the day ends with unfinished tasks in Today, Vega shows a simple bottom sheet for each unresolved task. Four options appear: move to tomorrow, keep in Today, mark as done, or delete. This review feels like a 30-second ritual, not a chore, and helps the user reset daily.

**Why this priority**: Unfinished tasks at end of day create psychological burden. A lightweight decision ritual clears the mind and resets Today for tomorrow.

**Independent Test**: Can be tested by verifying that at end-of-day (configurable time), unfinished Today tasks trigger one bottom sheet per task with 4 options, and user choices are applied correctly.

**Acceptance Scenarios**:
1. **Given** user has 2 unfinished tasks in Today at end of day, **When** end-of-day trigger fires, **Then** user sees 2 sequential bottom sheets (one task per sheet)
2. **Given** bottom sheet for an unfinished task is showing, **When** user selects "move to tomorrow", **Then** task moves to Today for next day
3. **Given** bottom sheet is open, **When** user selects "keep in Today", **Then** task remains in Today and ritual advances to next task
4. **Given** user has completed the snooze ritual, **When** ritual ends, **Then** Today view refreshes to show updated list

---

### User Story 6 — Swipe Gestures for Task Actions (Priority: P2)

From any task in a list, user can swipe right to mark done, or swipe left to reveal postpone and delete options. Long press opens detail/edit sheet. This keeps common actions fast and discoverable.

**Why this priority**: Touch gestures are natural on mobile and reduce tap count. Swipe right to done is a universal pattern that reduces friction for the most common action.

**Independent Test**: Can be tested by verifying swipe right marks task done, swipe left reveals actions, and long press opens edit sheet without requiring menus or extra navigation.

**Acceptance Scenarios**:
1. **Given** task is visible in a list, **When** user swipes right on task, **Then** task is marked done and moves to Done view
2. **Given** task is visible, **When** user swipes left, **Then** postpone and delete buttons appear
3. **Given** task is visible, **When** user long presses, **Then** detail/edit sheet opens showing all five fields
4. **Given** detail sheet is open, **When** user edits any field and closes sheet, **Then** changes are saved immediately

---

### User Story 7 — Global Search (Priority: P2)

A search icon in the top bar allows user to find any task across all views by title keyword. Search results display as a flat list with task state (Inbox, Today, Upcoming, Done) shown as a label.

**Why this priority**: Users build up task lists over time. Search helps retrieve forgotten tasks without navigating through all views manually.

**Independent Test**: Can be tested by verifying that search is accessible from top bar, returns tasks matching keyword across all views, and displays view state label for each result.

**Acceptance Scenarios**:
1. **Given** user taps search icon, **When** keyboard opens, **Then** they can type a keyword
2. **Given** user types "dentist", **When** search executes, **Then** all tasks with "dentist" in title appear (across all views)
3. **Given** search results show multiple tasks, **When** user views results, **Then** each task shows its current state (e.g., "Inbox", "Today", "Done")
4. **Given** search result is shown, **When** user taps a result, **Then** they jump to that task in its view

---

### User Story 8 — Gentle Reminders (Priority: P3)

Vega sends only non-intrusive nudges, not marketing notifications. Two permitted nudge types: "You still have tasks in Today" (once in early evening if unfinished), and a weekly review prompt on Sunday evening. Reminders are optional and can be disabled.

**Why this priority**: Notifications are a trust issue. Overuse damages the app. Light reminders at critical moments (end of day, weekly review) reinforce habit without addiction mechanics.

**Independent Test**: Can be tested by verifying that reminders are sent at correct times, only two types exist, reminders respect user preference toggle, and frequency is limited (once per day for daily, once per week for weekly).

**Acceptance Scenarios**:
1. **Given** user has unfinished tasks in Today, **When** early evening arrives (e.g., 6pm), **Then** one reminder notification appears
2. **Given** reminder is sent, **When** user taps it, **Then** they jump to Today view
3. **Given** reminders are enabled, **When** user opens Settings, **Then** they can toggle notifications on/off
4. **Given** Sunday evening arrives, **When** week ends, **Then** weekly review prompt appears asking user to review Inbox

---

### User Story 9 — Theme Support (Light/Dark) (Priority: P3)

App supports both light and dark themes natively, defaulting to system preference. All Material Design 2 components and colors adapt to selected theme.

**Why this priority**: Theme support is table-stakes for modern apps and respects user accessibility preferences. Lower priority because it does not affect core task management workflow.

**Independent Test**: Can be tested by verifying that app renders in both themes, theme follows system preference by default, user can override in Settings, and all UI elements are legible in both themes.

**Acceptance Scenarios**:
1. **Given** system is set to light theme, **When** user opens Vega for first time, **Then** app appears in light theme
2. **Given** system is set to dark theme, **When** user opens Vega, **Then** app appears in dark theme
3. **Given** user is in Settings, **When** they change theme preference, **Then** app switches immediately
4. **Given** app is in dark theme, **When** user views all screens, **Then** all text is readable and component contrast meets accessibility standards

---

### Edge Cases

- What happens when user types only a date in quick add ("tomorrow") with no task title? → Task is created with title blank, left for user to edit
- What happens if user has 100 tasks in Upcoming? → Upcoming view scrolls normally; no cap is enforced (cap only applies to Today)
- What happens if user tries to move a task to Today after 11:59pm? → Task is added to Today for the current calendar day; at midnight, it remains in Today until user explicitly moves it
- What happens if user tries to search with no results? → Search displays "No tasks found. Try a different keyword."
- What happens if user dismisses Next Best Action card and then immediately moves another task to Today? → Card refreshes to show new best action for the updated list
- What happens if user is offline and tries to capture a task? → Task is captured and stored locally; sync happens when connection returns (offline-first design)

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide a floating action button accessible from every screen that opens quick add
- **FR-002**: System MUST support natural language parsing of task title, due date, time, and priority from a single text input
- **FR-003**: System MUST capture any task without blocking on unparseable fields; unparseable fields default to blank
- **FR-004**: System MUST display Today view as the default landing screen when user opens the app
- **FR-005**: System MUST enforce a soft cap on Today view: display max 3–7 tasks by default, with overflow indicator for additional tasks
- **FR-006**: System MUST calculate and surface one Next Best Action task at the top of Today view, weighted by overdue status (highest), priority, due date proximity, and time of day
- **FR-007**: System MUST allow user to swipe right on any task to mark it done
- **FR-008**: System MUST allow user to swipe left on any task to reveal postpone and delete options
- **FR-009**: System MUST allow user to long press any task to open a detail/edit sheet showing all five fields (title, due date, priority, state, notes)
- **FR-010**: System MUST implement exactly four views (Inbox, Today, Upcoming, Done) accessible via bottom navigation bar
- **FR-011**: System MUST move new tasks to Inbox by default
- **FR-012**: System MUST allow user to manually move tasks between views; moves are instant
- **FR-013**: System MUST provide a global search from top bar that finds tasks across all views by title keyword
- **FR-014**: System MUST display search results with task state label (Inbox, Today, Upcoming, Done)
- **FR-015**: System MUST show an end-of-day snooze prompt for each unfinished task in Today with options: move to tomorrow, keep in Today, mark done, delete
- **FR-016**: System MUST send a "You still have tasks in Today" reminder once in early evening (e.g., 6pm) if tasks remain unfinished
- **FR-017**: System MUST send a weekly review prompt on Sunday evening asking user to review Inbox
- **FR-018**: System MUST allow user to disable reminders in Settings
- **FR-019**: System MUST support both light and dark themes, defaulting to system preference
- **FR-020**: System MUST enforce flat task data model: no subtasks, no tags, no labels, no nested projects in V1

### Key Entities

- **Task**: Represents a single unit of work. Contains: title (required), due date (optional), priority (optional, defaults to none), state (inbox/today/upcoming/done), notes (optional). No relational nesting.
- **View**: One of four fixed workflow states: Inbox (entry point), Today (focused work), Upcoming (future tasks), Done (completed archive). Views are not customizable.
- **Next Best Action**: A computed recommendation surfaced at the top of Today. Calculated from task properties (overdue status, priority, due date proximity) and context (current time). Not persisted; recalculated on each Today view load.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: New user can capture their first task in under 3 seconds after opening the app (including time to discover floating action button)
- **SC-002**: User can review and snooze unfinished Today tasks in under 30 seconds per task (end-of-day ritual)
- **SC-003**: User never sees more than 7 tasks in Today view by default (hard cap prevents cognitive overload)
- **SC-004**: Global search returns matching results in under 500ms for 1000+ task database
- **SC-005**: App launches and is interactive in under 1 second on mid-range device
- **SC-006**: Swipe gestures (mark done, postpone, delete) respond to user touch in under 200ms
- **SC-007**: 90% of new users complete their first Today view review without guidance
- **SC-008**: Natural language parser correctly extracts title, date, and priority from 95% of natural language inputs (measured across minimum 20 test cases)
- **SC-009**: End-of-day snooze prompt appears within 1 minute of configured end-of-day time
- **SC-010**: User satisfaction score on "I trust this app with all my tasks" is 8/10 or higher (measured via optional survey)

---

## Assumptions

- **Target User**: Working professional with 5–20 tasks per day; primary use case is capturing tasks during busy workdays (aligns with constitution recommendation)
- **Device**: Mobile-first design; responsive for phone screens; tablet and desktop support is not in scope for V1
- **Connectivity**: App must be fully functional offline (local-first); syncing to remote storage happens silently in background if applicable
- **Platform**: Native iOS and Android implementation (cross-platform framework selection deferred to planning phase); Material Design 2 system as design language ensures consistency
- **Data Storage**: Local storage using SQLite or equivalent; no user accounts required for V1; user's task data lives on their device
- **Notifications**: Only two notification types; opt-in; no analytics tracking or marketing notifications
- **Accessibility**: App must meet WCAG 2.1 AA standards; text is legible in both light and dark themes
- **No Recurring Tasks in V1**: Recurring task support is explicitly deferred to V2; users must manually recreate or re-add recurring tasks for now
- **No Calendar Integration**: Calendar sync is out of scope for V1; tasks can have due dates but no bidirectional calendar sync
- **No Collaboration**: Sharing, collaboration, and multi-user features are not in V1; this is a personal task manager
- **Natural Language Parser Baseline**: Parser is required to handle at minimum: task title extraction, date/time keywords (today, tomorrow, next Monday, in 3 days, 3pm), and priority keywords (high, urgent, low). Ambiguity is handled gracefully by capturing task with blank fields rather than blocking
