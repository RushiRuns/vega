# Feature Specification: Recurring Tasks

**Feature Branch**: `002-recurring-tasks`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: "implement the recurring task feature."

---

## User Scenarios & Testing

### User Story 1 — Quick Capture Recurring Tasks with Natural Language (Priority: P1)

A user wants to establish a new recurring routine quickly. They tap the floating action button and type "Read book every day high" or "Submit expense report every week". The system extracts the title ("Read book" / "Submit expense report"), sets the initial due date (today / today or upcoming scheduled day), sets the priority (high / none), and parses the recurrence interval (`DAILY` / `WEEKLY`).

**Why this priority**: Fast capture is the core engine of Vega. If the user cannot quickly capture recurring habits using natural language, the feature will feel disconnected and slow.

**Independent Test**: Can be tested by typing text containing "every day", "daily", "every week", "weekly", "every weekday", "every month", "monthly" into the quick add input and verifying that the corresponding recurrence chip is shown in the preview and saved correctly on the task.

**Acceptance Scenarios**:
1. **Given** user is on quick add, **When** they type "Meditate every day", **Then** the recurrence preview chip shows "Daily" and task is created with recurrence set to `DAILY`.
2. **Given** user is on quick add, **When** they type "Pay bills every month", **Then** the recurrence preview chip shows "Monthly" and task is created with recurrence set to `MONTHLY`.
3. **Given** user is on quick add, **When** they type "Write status report every Friday", **Then** the recurrence preview chip shows "Weekly", the due date is set to the upcoming Friday, and task is created with recurrence set to `WEEKLY`.

---

### User Story 2 — Zero-Friction Automated Next Occurrence Generation (Priority: P1)

A user marks their today's recurring task "Gym" (due today, daily recurrence) as done. The task is immediately moved to the `Done` state and saved. Simultaneously, a brand new task instance "Gym" is created. Its due date is set to tomorrow, its recurrence is set to `DAILY`, its priority and notes are copied, and its state is set to `UPCOMING` (or `TODAY` if tomorrow has already started relative to the user's current time). The user is never prompted to input anything; the schedule manages itself.

**Why this priority**: Minimalist task management means reducing the cognitive load. Automating the next occurrence creation ensures that recurring tasks do not require constant maintenance.

**Independent Test**: Can be verified by completing a task that has a recurrence pattern and checking that a new task with a shifted due date is instantly added to the database and displayed in the appropriate view.

**Acceptance Scenarios**:
1. **Given** a task "Brush teeth" (due today, recurrence `DAILY`, state `TODAY`), **When** user marks it done, **Then** it moves to Done, and a new task "Brush teeth" is created with due date set to tomorrow, recurrence `DAILY`, and state `UPCOMING`.
2. **Given** a task "Timesheet" (due Friday, recurrence `WEEKLY`, state `UPCOMING`), **When** user marks it done (even if done late on Saturday), **Then** it moves to Done, and a new task "Timesheet" is created with due date set to next Friday, recurrence `WEEKLY`, and state `UPCOMING`.

---

### User Story 3 — Edit Recurrence in Task Details (Priority: P2)

A user wants to change the frequency of a task or stop it from repeating. They long-press a task to open the details bottom sheet. They see a "Recurrence" drop-down displaying the current recurrence status. They click it and change it from "Daily" to "Weekly", or to "None" (stopping the recurrence), and click Save.

**Why this priority**: Life schedules change. Users need an easy, discoverable way to modify the repeat pattern of their tasks.

**Independent Test**: Can be tested by opening Task Details on a task, changing the Recurrence drop-down selection, saving, and verifying the updated recurrence value in the list or database.

**Acceptance Scenarios**:
1. **Given** a task with recurrence `DAILY`, **When** user opens Task Details, **Then** the Recurrence field shows "Daily".
2. **Given** user changes Recurrence from "Daily" to "Weekly" and clicks Save, **When** task is saved, **Then** recurrence is updated to `WEEKLY` in the database.
3. **Given** user changes Recurrence to "None" and clicks Save, **When** task is saved, **Then** recurrence is set to `null` in the database and the task behaves as a one-time task.

---

### Edge Cases

- **What happens when completing a weekday task on Friday?**
  - Next due date is calculated as next Monday (Friday + 3 days), since Weekdays recurrence skips Saturday and Sunday.
- **What happens if a recurring task has no due date?**
  - If a user sets a task to repeat but doesn't specify a starting due date, the first occurrence defaults to being due today.
- **What happens when deleting a recurring task?**
  - Only the current occurrence is deleted. The parent/template relationship is flat (each spawned task is independent). Deleting it stops future generations because the active occurrence is gone. This keeps the data model extremely simple.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST support natural language parsing of recurrence keywords (`every day`, `daily`, `every weekday`, `every week`, `weekly`, `every month`, `monthly`).
- **FR-002**: System MUST display a parsed recurrence preview chip in the Quick Add sheet.
- **FR-003**: System MUST persist the recurrence rule (`DAILY`, `WEEKDAYS`, `WEEKLY`, `MONTHLY`) as a string attribute on the `Task` entity.
- **FR-004**: System MUST allow editing of a task's recurrence rule via a dropdown spinner in the Task Details bottom sheet.
- **FR-005**: When a task with a recurrence rule is completed (moves to `DONE`), the system MUST automatically generate the next occurrence.
- **FR-006**: The next occurrence's due date MUST be computed based on the previous occurrence's due date (or completion date if no due date was set) plus the recurrence interval.
- **FR-007**: The next occurrence's state MUST be set to `TODAY` if its calculated due date is today or in the past, or `UPCOMING` if it is in the future. It MUST skip `INBOX` to prevent backlog clutter.
- **FR-008**: The new occurrence MUST copy the title, priority, notes, and recurrence rule from the completed task.
- **FR-009**: Completing a task in `DONE` state MUST NOT trigger a new occurrence (only transitions to `DONE` trigger recurrence).

### Key Entities

- **Task**: Contains a new optional `recurrence` attribute (String, nullable) representing the repeat interval (`DAILY`, `WEEKDAYS`, `WEEKLY`, `MONTHLY`).

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Setting a recurrence pattern during Quick Add adds under 200ms of overhead to natural language parsing.
- **SC-002**: Spawning the next occurrence after marking a task completed takes under 100ms and operates seamlessly on the background database thread without causing UI stuttering.
- **SC-003**: A user can change the recurrence settings of any task in the details sheet in under 3 taps.

---

## Assumptions

- **Flat Data Model**: We do not create separate "Template" and "Instance" tables. Each active task instance acts as the carrier of the recurrence rule. This keeps database queries simple and maintains local storage performance.
- **No Historic Adjustments**: Marking a past task completed doesn't retrospectively generate skipped occurrences. It only generates the single next valid occurrence.
