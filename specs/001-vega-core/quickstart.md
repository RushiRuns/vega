# Quickstart: Vega — Minimalist Personal Task Manager

**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)  
**Data Model**: [data-model.md](data-model.md)  
**Phase**: 1 (Design)

---

## Overview

This guide validates the Vega feature end-to-end through concrete user scenarios. Each scenario is independently testable and demonstrates that the core feature delivers value.

---

## Validation Scenarios

### Scenario 1: New User, First Task Capture

**Goal**: Verify that a new user can capture a task in under 3 seconds with natural language parsing

**Preconditions**:
- App freshly installed
- All views empty (no existing tasks)
- User is on any screen

**Setup**:
- Open Vega app

**Steps**:

1. **Tap floating action button** → Quick Add bottom sheet appears
2. **Type text**: "Buy milk tomorrow 10am"
3. **Confirm** → Task is created instantly

**Validation**:

- ✅ Bottom sheet was visible within 500ms of tap
- ✅ Text input focused and keyboard visible
- ✅ Task was captured without explicit date/priority selection
- ✅ Natural language parser extracted:
  - Title: "Buy milk"
  - Due date: Tomorrow at 10:00 AM
  - Priority: NONE (default)
- ✅ Task appears in Inbox view with due date visible
- ✅ Total time from tap to confirmation: <3 seconds

**Expected Result**: Inbox contains 1 task with correct title and due date. User can immediately continue to next capture.

---

### Scenario 2: Today List Cap & Overflow Behavior

**Goal**: Verify that Today view enforces cognitive load cap (3–7 tasks) with graceful overflow

**Preconditions**:
- Vega has 8 tasks in Inbox (created by moving them there)
- User can move tasks to Today view

**Setup**:
- Create 8 tasks via Quick Add
- Manually move all 8 to Today view

**Steps**:

1. **Navigate to Today** → View shows first 7 tasks clearly
2. **Scroll down** → "+1 more" indicator visible below 7th task
3. **Scroll more** → 8th task becomes visible
4. **Scroll back up** → Top 7 tasks are shown, 8th is hidden
5. **Try to add 9th task and move to Today** → Warning message appears: "You have a lot in Today. Consider focusing on fewer tasks."

**Validation**:

- ✅ By default, exactly 7 tasks are shown (or fewer if fewer exist)
- ✅ Overflow count "+1 more" is visible and clickable
- ✅ Tapping overflow indicator reveals hidden tasks
- ✅ Adding 8th+ task triggers a gentle warning, not a hard block
- ✅ User can still proceed to add if they want

**Expected Result**: Today list never feels overwhelming by default. User has agency to expand if they choose.

---

### Scenario 3: Next Best Action Card Selection

**Goal**: Verify that the Next Best Action card surfaces the highest-priority task correctly

**Preconditions**:
- Today has exactly 3 tasks:
  - Task A: Overdue (due date = yesterday), priority = NONE
  - Task B: Due in 30 minutes, priority = LOW
  - Task C: Due tomorrow, priority = HIGH
- Current time is morning (e.g., 9:00 AM)

**Setup**:
- Create 3 tasks with specified due dates and priorities
- Move all to Today

**Steps**:

1. **Navigate to Today** → Next Best Action card displays at top
2. **Verify card shows Task A** (overdue) → Scoring weights overdue status at +100, highest weight
3. **Dismiss card** (swipe or close button) → Card disappears
4. **Refresh view** → Task A reappears (card is recalculated, not permanently dismissed)
5. **Mark Task A as done** → Card refreshes to show Task C (due tomorrow, highest remaining priority)

**Validation**:

- ✅ Next Best Action card is always visible at top of Today list
- ✅ Overdue task is prioritized highest (scores +100)
- ✅ Dismissing card removes it temporarily
- ✅ Completing the recommended task updates card to next best action
- ✅ Card appears as visually distinct (elevated, different color scheme)
- ✅ Card does not feel "AI-powered" or algorithmic — just the most relevant task

**Expected Result**: User always knows which task to tackle first. Card feels intuitive, not intrusive.

---

### Scenario 4: Swipe Gestures (Mark Done, Postpone, Delete)

**Goal**: Verify that touch gestures work smoothly and respond in <200ms

**Preconditions**:
- Inbox or Today view has 3+ tasks
- Tasks are visible in a RecyclerView

**Setup**:
- Navigate to a view with multiple tasks

**Steps**:

1. **Swipe right on Task A** → Task slides right, fades, moves to Done view
   - Response time: Measure <200ms from touch to visual confirmation
   - Task immediately removed from current view
   - Task appears in Done view

2. **Swipe left on Task B** → Reveal two action buttons: Postpone, Delete
   - Response time: <200ms from touch to action button reveal
   - Buttons appear with clear icons (calendar icon for Postpone, trash icon for Delete)
   - User can tap to select action

3. **Tap Postpone on Task B** → Task moves to Upcoming (or tomorrow if in Today)
   - Task disappears from current view
   - Task appears in Upcoming with updated due date

4. **Tap Delete on Task C** → Confirmation prompt (optional): "Delete this task?"
   - If confirmed, task is removed from all views
   - Task does not appear in Done (it's deleted, not completed)

5. **Long-press on Task D** → Bottom sheet opens showing full task detail
   - All 5 fields are displayed: title, due date, priority, state, notes
   - User can edit any field
   - Save button commits changes

**Validation**:

- ✅ Swipe right: response <200ms, visual feedback immediate
- ✅ Swipe left: response <200ms, action buttons appear with icons
- ✅ Postpone: task moves to correct view, due date updated
- ✅ Delete: task removed with optional confirmation
- ✅ Long-press: detail sheet opens, all fields are editable
- ✅ No lag or jank on mid-range device

**Expected Result**: All interactions feel instant and responsive. User never waits for UI feedback.

---

### Scenario 5: Natural Language Parsing Edge Cases

**Goal**: Verify that the parser handles ambiguous input gracefully without blocking capture

**Preconditions**:
- Quick Add bottom sheet is open
- Parser is ready to process various inputs

**Setup**:
- Open Quick Add

**Test Cases**:

| Input Text | Expected Title | Expected Due Date | Expected Priority | Behavior |
|---|---|---|---|---|
| "Buy milk" | "Buy milk" | null | NONE | Simple title only |
| "Call mom tomorrow 3pm" | "Call mom" | Tomorrow 3pm | NONE | Full parse |
| "High priority fix bug" | "Fix bug" | null | HIGH | Title + priority |
| "Next Monday" | null (or "Next Monday") | Next Monday | NONE | Just date, title blank |
| "Urgent: send email" | "Send email" | null | HIGH | Title + priority keyword |
| "Tomorrow" | "Tomorrow" | null | NONE | Ambiguous — treat as title |
| "123 very important task" | "123 very important task" | null | NONE | No keywords — all becomes title |
| "" (empty) | (task not created) | — | — | Empty input blocked by validation |
| "in 3 days 10am medium priority" | Remaining text after parsing | In 3 days 10am | MEDIUM | Complex parsing |

**Validation**:

- ✅ Parser handles 95%+ of natural inputs correctly
- ✅ Ambiguous input is captured anyway (doesn't block user)
- ✅ Unparseable fields are left blank, user can edit later
- ✅ Every test case above produces a valid task (except empty)

**Expected Result**: Parser is intelligent but never perfectionist. Capture is never blocked.

---

### Scenario 6: End-of-Day Snooze Ritual

**Goal**: Verify that unfinished Today tasks trigger a guided review ritual at day's end

**Preconditions**:
- Today is 21:00 (9 PM) — end-of-day trigger time
- Today has 2 unfinished tasks:
  - Task A: "Finish report"
  - Task B: "Email client"
- Remaining unfinished

**Setup**:
- Add 2 tasks to Today
- Do not complete them
- Wait for 21:00 (or manually trigger end-of-day in test)

**Steps**:

1. **At 21:00, snooze prompt fires** → Bottom sheet appears for Task A
   - Title: "Finish report"
   - Options presented as 4 buttons:
     - Move to Tomorrow
     - Keep in Today
     - Mark as Done
     - Delete
   - Brief visual/haptic feedback

2. **User selects "Move to Tomorrow"** → Task A is updated with due date = tomorrow
   - Sheet dismisses
   - Next sheet appears for Task B

3. **User selects "Mark as Done"** → Task B is marked DONE
   - Sheet dismisses
   - If more tasks, next sheet appears
   - If no more tasks, ritual ends

4. **Ritual complete** → Today view refreshes
   - Moved tasks appear with updated dates
   - Done tasks are gone (moved to Done view)
   - Screen shows empty or updated Today list

**Validation**:

- ✅ Snooze triggered at configured time (default 21:00)
- ✅ One sheet per unfinished task (sequential, not all at once)
- ✅ Four options clearly presented
- ✅ User choices are applied correctly
- ✅ Entire ritual takes <30 seconds for 2 tasks
- ✅ Today view reflects changes immediately after

**Expected Result**: User reviews unfinished tasks in a lightweight ritual. Today is reset for tomorrow.

---

### Scenario 7: Offline Functionality (No Network Required)

**Goal**: Verify that Vega functions completely offline with no network calls

**Preconditions**:
- Vega is open and populated with tasks
- Device has network disabled (airplane mode)

**Setup**:
- Enable airplane mode
- Verify no network connectivity

**Steps**:

1. **Create a new task** → Complete Quick Add workflow
   - Task is captured and appears in Inbox
   - No network latency, no waiting for sync

2. **Move task to Today** → Task moves immediately
   - No sync call blocking the action
   - Optimistic update visible instantly

3. **Edit task** → Open detail sheet, change title, save
   - Changes persist immediately
   - No remote sync hanging the UI

4. **Swipe to mark done** → Task moves to Done view
   - Response is immediate
   - No cloud sync required

5. **Search for a task** → Search results appear instantly
   - Local database query completes in <100ms
   - No network call needed

6. **Disable airplane mode** → Reconnect to network
   - App continues working exactly the same
   - No sync conflict, no forced update
   - (In future version, background sync could happen here)

**Validation**:

- ✅ All operations complete instantly with no network
- ✅ No spinners, no loading screens
- ✅ Data persists across app restarts
- ✅ App functions identically online and offline

**Expected Result**: Vega is truly offline-first. Network is never a bottleneck.

---

### Scenario 8: Theme Support (Light & Dark)

**Goal**: Verify that light and dark themes render correctly and follow system preference

**Preconditions**:
- Vega is installed
- Device has light/dark theme setting

**Setup**:
- Set device to light theme

**Steps**:

1. **Open Vega in light theme** → App renders in light colors
   - Background: light gray/white
   - Text: dark gray/black, high contrast
   - FAB, buttons, cards: Material light theme colors
   - All text is legible

2. **Go to Settings → Theme** → Theme picker appears
   - Options: System (default), Light, Dark

3. **Select Dark theme** → App switches immediately
   - Background: dark gray/black
   - Text: light gray/white, high contrast
   - All Material components adapt
   - All text is legible

4. **Verify all screens** → Navigate to each view (Inbox, Today, Upcoming, Done)
   - All views render correctly in both themes
   - No broken colors, no unreadable text
   - Elevated cards have appropriate shadow depth

5. **Set device to dark mode, restart app** → App starts in dark theme (follows system)

**Validation**:

- ✅ Light theme is readable and Material-correct
- ✅ Dark theme is readable and Material-correct
- ✅ Theme toggle is instant and affects entire app
- ✅ System preference is respected on app start
- ✅ No WCAG 2.1 AA contrast violations

**Expected Result**: User can use Vega comfortably in light or dark mode.

---

## Acceptance Criteria (All Scenarios)

A Vega build is acceptable when:

1. ✅ All 8 scenarios complete successfully
2. ✅ No crashes, ANRs (Application Not Responding), or runtime errors
3. ✅ No unreadable text in either theme
4. ✅ All response times meet targets (<3s capture, <200ms swipes)
5. ✅ Offline mode works without network enabled
6. ✅ App launches and is interactive in <1 second
7. ✅ Natural language parser handles all test inputs gracefully

---

## Deployment Validation Checklist

Before shipping Vega:

- [ ] All 8 scenarios pass on Android 8.0 (minimum SDK)
- [ ] All scenarios pass on Android 14 (target SDK)
- [ ] Tested on 3+ device form factors (phone, tablet)
- [ ] Tested on mid-range device (performance targets verified)
- [ ] Offline scenario verified (no network dependencies)
- [ ] Theme validation complete (light + dark)
- [ ] Parser test suite all 20+ cases pass
- [ ] No ANRs, crashes, or data loss observed
- [ ] Unit tests for core actions: capture, move, snooze, scoring (all green)

---

**Status**: ✅ Quickstart validation scenarios complete

**Next**: Implementation planning (tasks.md — generated by `/speckit.tasks`)
