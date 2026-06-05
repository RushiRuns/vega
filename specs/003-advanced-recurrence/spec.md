# Feature Specification: Advanced Recurring Tasks

**Feature Branch**: `003-advanced-recurrence`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: "so in google tasks, there is sophisticated system for recurring tasks as shown in the image. create the similar system."

---

## User Scenarios & Testing

### User Story 1 — Setup Custom Daily/Weekly/Monthly/Yearly Repeats (Priority: P1)

A user wants to establish a task that repeats every 2 weeks on Monday and Friday, or every 3 months on the first Friday. They long-press a task to open the details bottom sheet and click on the "Recurrence" field. This opens the "Repeats" screen. They configure:
1. **Every**: 2 weeks.
2. **Weekday selector**: Toggles on M and F.
3. **Set time**: 10:00 AM.
4. **Starts**: 5 June.
5. **Ends**: Never.
They click "Done" at the top right, returning to the details sheet. The recurrence field displays the summary: "Every 2 weeks on Mon, Fri". When they save, this rule is serialized and saved.

**Why this priority**: Custom repeat intervals are crucial for scheduling professional routines (e.g. bi-weekly payroll, quarterly reports).

**Independent Test**: Verify that the Repeats Dialog correctly captures all selections and returns a valid rule containing the corresponding settings (interval, frequency, selected weekdays, starts/ends), updating the task details summary view.

**Acceptance Scenarios**:
1. **Given** user is on Repeats Dialog, **When** they select Every `2` `week` and toggle `M` and `F`, **Then** the recurrence rule is generated with frequency `WEEKLY`, interval `2`, and weekdays `[Monday, Friday]`.
2. **Given** user selects Every `3` `month` and selects "First Friday", **Then** the recurrence rule is generated with frequency `MONTHLY`, interval `3`, monthlyType `DAY_OF_WEEK`, dayOfWeekOccurrence `1`, and dayOfWeek `Friday`.

---

### User Story 2 — Set End Conditions: Never, On Date, or After Occurrences (Priority: P1)

A user schedules a workout routine "Cardio" repeating every day, but only for the summer (ends on August 31st) or for 12 sessions (ends after 12 occurrences). They toggle the "Ends" options in the Repeats Dialog to "On August 31" or "After 12 occurrences". The task repeats accordingly and automatically terminates once the criteria is met.

**Why this priority**: Routines are often bounded by deadlines or caps. Having tasks recur infinitely creates chore list fatigue.

**Independent Test**: Verify that completing the last occurrence (matching the end date or occurrences limit) does NOT generate any further task instances.

**Acceptance Scenarios**:
1. **Given** a task with recurrence set to end after 2 occurrences, **When** user completes the first instance, **Then** the 2nd instance is generated. **When** user completes the 2nd instance, **Then** no 3rd instance is generated.
2. **Given** a task set to end on June 10th, **When** the next occurrence's calculated due date is June 11th, **Then** no new instance is generated upon completion of the June 10th instance.

---

### Edge Cases

- **What happens if a user completes a weekday task late?**
  - The calculation should hop to the next scheduled weekday (e.g. Mon, Wed, Fri schedule completed on Tuesday hops to Wednesday).
- **What happens if the start date is in the future?**
  - The task's first occurrence due date starts on the start date, and subsequent recurrences shift from there.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST store the recurrence settings as a serialized JSON string in the `recurrence` column.
- **FR-002**: System MUST calculate the next occurrence due date taking into account custom intervals, selected weekdays, and monthly relative rules.
- **FR-003**: System MUST support termination conditions: `Never`, `On Date` (timestamp limit), and `After occurrences` (counter limit).
- **FR-004**: System MUST display a dedicated "Repeats" configuration dialog with:
  - Custom interval text input and frequency dropdown (day, week, month, year).
  - Weekday selection chips for weekly tasks.
  - Radio options for specific day of month (e.g. "Day 5") vs relative weekday (e.g. "First Friday") for monthly tasks.
  - Date selectors for start and end dates.
  - Occurrence count input for "After X occurrences".
- **FR-005**: System MUST render a friendly text summary of the recurrence rule in the Task Details view.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Next due date calculation executes in under 50ms for any rule complexity.
- **SC-002**: Repeats dialog handles unit changes dynamically (hiding/showing weekdays and monthly options in under 100ms).
