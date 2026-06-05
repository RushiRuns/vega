<!-- 
SYNC IMPACT REPORT
==================
Version Change: 1.1.0 → 1.2.0 (ADVANCED RECURRING TASKS RATIFICATION)
Ratification Date: 2026-06-04
Last Amended: 2026-06-05

SECTIONS DEFINED:
- Core Product Philosophy (defines core mission)
- UX Principles (2.1 Friction, 2.2 Main Screen, 2.3 Today/Intelligence, 2.4 Cognitive Load)
- Feature Governance (3.1 Task Fields, 3.2 Workflow, 3.3 Quick Add, 3.4 Next Best Action)
- Out of Scope (4.0 V1 Constraints)
- Technical Principles (5.1 Performance, 5.2 Offline-First, 5.3 Data Simplicity, 5.4 No Bloat)
- Code Quality Standards (6.0)
- Decision-Making Hierarchy (7.0)
- Target User Definition (8.0)

DEPENDENT TEMPLATES UPDATED:
✅ plan-template.md - aligns with core mission and technical principles
✅ spec-template.md - scope constraints map to Feature Governance
✅ tasks-template.md - task types reflect principle-driven categories
⚠️ checklist-template.md - review for alignment with quality standards
⚠️ command templates - verify no outdated references to specific frameworks

FOLLOW-UP TODOs:
- [ ] Target User persona finalization (section 8: recommendation = working professional)
- [ ] Technical architecture decision (local vs. remote storage implementation)
- [ ] Natural language parser design spec (21+ test cases required by principle 6)

VERSION RATIONALE:
MAJOR: Initial ratification of governing constitution; establishment of all core principles.
-->

# Minimalist Task Manager App — Constitution

> This constitution is the governing document for all specification, planning, and implementation decisions. Every technical choice, UX decision, and feature addition must be evaluated against these principles.

---

## 1. Core Product Philosophy

**The fewer decisions the user makes, the more productive they become.**

The app exists to help users *decide what to do next* — not just store tasks. Every feature, interaction, and screen must serve this goal. If a proposed change makes the user think more, reconsider it.

Trust is the primary product metric. A user who trusts the app keeps everything in it. A user who keeps everything in it uses it every day. Design for trust first, features second.

---

## 2. UX Principles

### 2.1 Friction is the Enemy

- Any action the user repeats daily must take under 3 seconds
- No modals, forms, or multi-step flows for task capture
- The app must never make the user feel lost — one obvious path forward at all times

### 2.2 The Main Screen Is Sacred

The entire app must be navigable from one central screen:

- **Top:** Current date + search
- **Middle:** Today list (max 3–7 tasks visible by default)
- **Bottom:** Floating add button
- **Gestures:** Swipe left/right for done, postpone, delete
- **Extended options:** Long press for additional actions

### 2.3 Today Must Always Answer: "What Should I Do Right Now?"

- Today view is the default landing screen, always
- Tasks outside Today must never bleed into focus unintentionally
- The Next Best Action card is the intelligence layer — surface it prominently

### 2.4 Cognitive Load Caps

- Cap Today to 3–7 tasks; warn when the user tries to exceed this
- Auto-snooze unfinished tasks at end of day with a simple 4-option prompt: move tomorrow / keep in Today / mark done / delete
- Never show overdue, upcoming, and today tasks in the same list

---

## 3. Feature Governance

### 3.1 A Task Has Exactly Five Fields

1. **Title** (required)
2. **Due date** (optional)
3. **Priority** — low / medium / high (optional, defaults to none)
4. **Next action state** — inbox / today / upcoming / done
5. **Notes** (optional, freeform)

**Constraint:** No subtasks, no tags, no labels, no folders, no projects in V1. Any request to add these must be deferred to a future version and documented in a backlog.

### 3.2 The Workflow Is Fixed

**Inbox → Today → Upcoming → Done**

This is not configurable by the user. Views map 1:1 to this workflow. No custom views, filters, or saved searches in V1.

### 3.3 Quick Add Is the Most Important Feature

Natural language parsing must handle at minimum:

- Task title extraction
- Date/time parsing (today, tomorrow, next Monday, 3pm, etc.)
- Priority keywords (high, urgent, low)

If parsing is ambiguous, default gracefully — never block capture.

### 3.4 Next Best Action Card

Surface one recommended task based on:

- Overdue status (highest weight)
- Priority
- Due date proximity
- Estimated effort (if available)
- Time of day context

This card must not feel like AI. It must feel like the app simply knows what matters.

### 3.5 Recurring Tasks Governance

- A recurring task is a schedule that automatically spawns a new task instance when the current active occurrence is marked done.
- System must support advanced recurrence configurations similar to Google Tasks: custom intervals, specific day-of-week selections for weekly tasks, monthly relative schedules (e.g., "first Friday"), and flexible termination rules (Never, On Date, After X Occurrences).
- To prevent cognitive overload, only the *current active occurrence* is visible in Today or Upcoming views. Future occurrences must never clutter these lists.
- Newly spawned occurrences skip the Inbox and are placed directly in Today or Upcoming depending on their due date.

---

## 4. What to Never Build (V1 Constraint List)

The following are explicitly out of scope for V1. Any implementation that introduces these must be flagged and rejected:

- Nested or hierarchical projects
- Tags, labels, or color categories
- Social features, sharing, or collaboration
- Gamification (streaks, points, badges)
- Custom views or filters
- Calendar integration (defer to V2)
- More than 4 navigation tabs
- Onboarding tutorials or feature tours
- Any screen that requires more than 2 taps to reach from the main view

---

## 5. Technical Principles

### 5.1 Performance Is a Feature

- App must load and be interactive in under 1 second on a mid-range device
- Task capture must never lag — optimistic UI updates are required
- All reads from local state first; sync is background-only

### 5.2 Offline First

- The app must be fully functional with no internet connection
- Sync to remote (if applicable) happens silently in the background
- No blocking spinners for any user-initiated action

### 5.3 Data Simplicity

- Task data model is flat — no relational complexity in V1
- Local storage or SQLite preferred over remote-only databases in V1
- No user accounts required for V1 (optional stretch goal)

### 5.4 No Bloat

- Minimize third-party dependencies
- No analytics SDKs that add startup cost
- No push notification frameworks beyond the native OS layer

---

## 6. Code Quality Standards

- Every core user action (add, complete, snooze, postpone) must have a corresponding unit test
- UI components must be isolated and independently testable
- No business logic in UI layer — strict separation of concerns
- All date/time operations must use a single utility module (no scattered native Date calls)
- Natural language parser must have its own test suite with at least 20 input cases

---

## 7. Decision-Making Governance

When a decision is unclear, apply this hierarchy in order:

1. **Does it reduce friction for the user?** → Favor it
2. **Does it help the user know what to do next?** → Favor it
3. **Does it add a field, setting, or option the user must manage?** → Reject it
4. **Has a simpler solution been considered?** → Choose the simpler one
5. **Does it belong in V2?** → Document it and defer

When in doubt, do less. A missing feature can be added. A cluttered interface erodes trust permanently.

---

## 8. Target User Profile

The constitution is valid for all three candidate personas but must be finalized for one before implementation begins:

| Persona | Key Need | Primary Tension |
|---------|----------|-----------------|
| **Working professional** | Quick capture during busy days | Deep work vs. task management overhead |
| **Student** | Deadline tracking across subjects | Volume of tasks vs. focus |
| **ADHD / overwhelm-focused** | Reduced decision fatigue | Too many options causing paralysis |

**Recommendation:** Start with the working professional. They have the highest willingness to pay, the clearest pain point (context switching), and the most predictable daily pattern. The app's design naturally serves ADHD users as a secondary benefit.

---

## Governance

### Constitution Authority

This constitution is the supreme governing document. All specification, planning, and implementation decisions must align with these principles. Technical choices not justified by reference to one or more principles must be rejected or deferred to V2.

### Amendment Process

1. Proposed amendment must cite which principle(s) it affects
2. Rationale for change must be documented (why the principle proved incorrect or insufficient)
3. Migration plan for affected work must be drafted
4. Version number must be incremented per semantic versioning rules
5. Last Amended date must be updated to current date

### Versioning

- **MAJOR:** Principle redefinition, removal, or backward-incompatible governance change
- **MINOR:** New principle or section addition; material expansion of guidance
- **PATCH:** Clarifications, wording refinements, typographical corrections

### Compliance Review

- All PRs should verify compliance against relevant principles (especially sections 2, 3, and 5)
- Complexity introduced by a feature must be justified by reference to section 7 decision hierarchy
- Code review must spot-check alignment with code quality standards (section 6)
- For guidance on runtime development practices, see `.github/copilot-instructions.md`

---

**Version:** 1.2.0 | **Ratified:** 2026-06-04 | **Last Amended:** 2026-06-05
