# Tasks: Vega — Minimalist Personal Task Manager

**Input**: Design documents from `specs/001-vega-core/`

**Prerequisites**: plan.md (architecture, tech stack), spec.md (user stories), data-model.md (entity schema), quickstart.md (validation scenarios)

**Organization**: Tasks grouped by user story (P1, P2, P3) to enable independent implementation and testing

**Testing**: Unit tests included (parser, ViewModels, repository, scoring); UI tests deferred to V2

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Android project initialization and core configuration

- [X] T001 Create Android project structure with app module and build configuration in `app/build.gradle.kts`
- [X] T002 Configure Gradle dependencies: Jetpack (Room, ViewModel, Navigation, WorkManager, Hilt), MDC-Android 1.x, Kotlin Coroutines, JUnit 4, Mockito in `app/build.gradle.kts`
- [X] T003 [P] Create `app/src/main/AndroidManifest.xml` with required permissions (RECEIVE_BOOT_COMPLETED for WorkManager, SCHEDULE_EXACT_ALARM)
- [X] T004 [P] Setup proguard rules in `app/proguard-rules.pro` for Room, Hilt, and Kotlin
- [X] T005 Create application class `app/src/main/java/com/vega/VegaApplication.kt` with Hilt @HiltAndroidApp annotation
- [X] T006 Configure Hilt dependency injection module in `app/src/main/java/com/vega/di/AppModule.kt` (database, DAO, repository providers)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Create Room database entity `Task` in `app/src/main/java/com/vega/data/database/Task.kt` with all 5 fields (title, dueDate, priority, state, notes) and audit timestamps
- [X] T008 Create Room DAO interface `TaskDao` in `app/src/main/java/com/vega/data/database/TaskDao.kt` with methods for CRUD, filtering by state, sorting, search
- [X] T009 Create Room database class `VegaDatabase` in `app/src/main/java/com/vega/data/database/VegaDatabase.kt` with migration configuration (none required for V1)
- [X] T010 Create `TaskRepository` abstraction in `app/src/main/java/com/vega/data/repository/TaskRepository.kt` wrapping DAO operations
- [X] T011 [P] Create `TaskPriority` enum in `app/src/main/java/com/vega/data/database/TaskPriority.kt` (NONE, LOW, MEDIUM, HIGH)
- [X] T012 [P] Create `TaskState` enum in `app/src/main/java/com/vega/data/database/TaskState.kt` (INBOX, TODAY, UPCOMING, DONE)
- [X] T013 Create theme configuration in `app/src/main/res/values/themes.xml` inheriting from `Theme.MaterialComponents.DayNight` for light/dark support
- [X] T014 [P] Create Material Design 2 colors in `app/src/main/res/values/colors.xml` (primary, secondary, error, surface) and dark variant in `app/src/main/res/values-night/colors.xml`
- [X] T015 [P] Create dimensions file `app/src/main/res/values/dimens.xml` with standard Material spacing (8dp, 16dp, 24dp) and component sizes
- [X] T016 Create strings file `app/src/main/res/values/strings.xml` with all UI labels (app name, view titles, button labels, error messages)
- [X] T017 Create navigation graph `app/src/main/res/navigation/nav_graph.xml` defining 4 destinations (Inbox, Today, Upcoming, Done) and Quick Add dialog
- [X] T018 Create bottom navigation menu `app/src/main/res/menu/bottom_nav_menu.xml` with 4 items mapping to destinations
- [X] T019 Create main activity layout `app/src/main/res/layout/activity_main.xml` with NavHostFragment, BottomNavigationView, and FloatingActionButton

**Checkpoint**: Foundation infrastructure complete — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 — Quick Task Capture with Natural Language (Priority: P1) 🎯 MVP

**Goal**: Enable users to capture tasks in under 3 seconds using natural language input. System extracts title, due date, time, and priority from freeform text. Capture never blocks.

**Independent Test**: Can capture task in <3 seconds, natural language parser correctly extracts title/date/priority, ambiguous input is captured with blank fields, task appears in Inbox immediately

### Tests for User Story 1 (Unit Tests — must FAIL before implementation)

- [X] T020 [P] [US1] Create natural language parser test suite in `app/src/test/java/com/vega/parser/NaturalLanguageParserTest.kt` with minimum 20 test cases covering: simple title, title+date, title+priority, title+date+time, ambiguous dates, priority keywords (high, urgent, low, medium, asap), date formats (today, tomorrow, next [day], in [N] days, MM/DD/YYYY, relative), time formats (3pm, 10:30, 3:30pm), edge cases (empty, special chars, multiple keywords, conflicting inputs)
- [X] T021 [P] [US1] Create `QuickAddViewModelTest.kt` in `app/src/test/java/com/vega/ui/viewmodels/` with tests: capture creates task in Inbox with parsed fields, empty title is rejected, task persists after confirmation, input field clears after confirmation
 
### Implementation for User Story 1
 
- [X] T022 Create `NaturalLanguageParser.kt` in `app/src/main/java/com/vega/parser/` with: extractPriority() function matching keywords (high, urgent, asap → HIGH; medium → MEDIUM; low → LOW; default NONE), extractDateTime() function parsing dates/times with fallback to null on ambiguity, extractTitle() function remaining text after tokens removed, ParseResult data class with nullable date/priority
- [X] T023 Create `ParseResult.kt` data class in `app/src/main/java/com/vega/parser/` containing title (String), dueDate (Long?), priority (TaskPriority?), with convenience methods isAmbiguous() and toString()
- [X] T024 Create `QuickAddViewModel` in `app/src/main/java/com/vega/ui/viewmodels/QuickAddViewModel.kt` with: parseInput(text) method using NaturalLanguageParser, createTask(title, dueDate, priority) calling repository, StateFlow<ParseResult> for real-time preview, UiState for sheet management (showing, loading, error)
- [X] T025 Create `QuickAddBottomSheetFragment.kt` in `app/src/main/java/com/vega/ui/fragments/` with: single TextInputEditText field, real-time parser preview row showing extracted date/priority as dismissible chips, confirm button calling ViewModel.createTask(), optimistic UI (dismiss immediately after confirm), input field clears after success
- [X] T026 Create bottom sheet layout `app/src/main/res/layout/bottom_sheet_quick_add.xml` with TextInputLayout containing TextInputEditText, horizontal chip container for preview, MaterialButton for confirm
- [X] T027 [P] Integrate FloatingActionButton in MainActivity to launch QuickAddBottomSheetFragment when tapped
- [X] T028 Add form validation in QuickAddViewModel: title must not be empty, reject task creation if title blank after parsing, show error message

**Checkpoint**: User Story 1 complete — users can capture tasks with natural language parsing, tasks appear in Inbox immediately

---

## Phase 4: User Story 2 — Today View with Focus Cap (Priority: P1)

**Goal**: Today is the default landing screen showing only 3–7 tasks. If more exist, show "+[N] more" indicator. User can scroll to expand or dismiss overflow.

**Independent Test**: Today is default view on app open, 7 or fewer tasks displayed by default, overflow indicator shown for 8+ tasks, tapping indicator reveals hidden tasks, cap is enforced but not a hard block

### Tests for User Story 2

- [X] T029 [P] [US2] Create `TodayViewModelTest.kt` with tests: getTodayTasks returns list, list is capped at 7 items by default, overflow count computed correctly, expanding view loads all tasks

### Implementation for User Story 2

- [X] T030 Create `TodayViewModel` in `app/src/main/java/com/vega/ui/viewmodels/TodayViewModel.kt` with: StateFlow<List<Task>> emitting Today tasks only, StateFlow<Int> for overflow count, StateFlow<Task?> for Next Best Action card, methods to move/complete/delete tasks, cap logic (show 7 by default, compute overflow)
- [X] T031 Create `TodayFragment.kt` in `app/src/main/java/com/vega/ui/fragments/TodayFragment.kt` with: observes TodayViewModel.todayTasks, displays with cap enforcement, shows overflow indicator conditionally, RecyclerView with swipe gestures enabled, Next Best Action card at top
- [X] T032 Create today view layout `app/src/main/res/layout/fragment_today.xml` with: MaterialCardView for Next Best Action at top, RecyclerView for task list, empty state view ("Nothing in Today"), overflow indicator chip at bottom
- [X] T033 [P] Add soft cap warning: when user attempts to add 8th task to Today, show snackbar message "You have a lot in Today. Consider focusing on fewer tasks." but allow user to proceed
- [X] T034 Create empty state view for Today: "Nothing in Today. Check Inbox or Upcoming for tasks to review." with appropriate styling

**Checkpoint**: User Story 2 complete — Today is focused, never overwhelming

---

## Phase 5: User Story 3 — Next Best Action Card (Priority: P1)

**Goal**: Top of Today view displays one highlighted card recommending the task user should do right now. Algorithm weights overdue (highest) → priority → due soon → due today.

**Independent Test**: Card displays highest-scoring task, overdue tasks always prioritized, dismissing card removes it temporarily, completing recommended task updates card, card recalculates on view refresh

### Tests for User Story 3

- [X] T035 [P] [US3] Create `NextBestActionScoringTest.kt` in `app/src/test/java/com/vega/ui/logic/` with tests: overdue task scores +100, HIGH priority scores +30, MEDIUM +15, LOW +5, NONE +0, due within 2 hours scores +40, due today scores +20, highest score selected, ties broken deterministically

### Implementation for User Story 3

- [X] T036 Create scoring function `NextBestActionScoringTest.scoreTask(task, now)` as pure Kotlin function in TodayViewModel returning weighted integer score: overdue +100, priority weights, due soon +40, due today +20, maximum across all Today tasks selected as best action
- [X] T037 Add StateFlow<Task?> nextBestAction to TodayViewModel, computed from todayTasks whenever list updates, recalculated on time change (via LiveData or manual refresh)
- [X] T038 Create `NextBestActionCardViewHolder` extending RecyclerView.ViewHolder rendering card with: task title, priority badge, due date display, elevated Material card styling, visually distinct from regular task cards
- [X] T039 Create card layout `app/src/main/res/layout/item_next_best_action_card.xml` as elevated MaterialCardView with: task title (larger font), priority indicator, due date, recommended tag/icon (optional), dismiss button
- [X] T040 Add dismiss functionality: user can swipe left or tap X to dismiss card for current session (removed from list), card reappears on view refresh or task completion
- [X] T041 [P] Update TodayFragment to render Next Best Action card at top using CardView before task list RecyclerView

**Checkpoint**: User Story 3 complete — Today view guides users to highest-priority task

---

## Phase 6: User Story 4 — Fixed Workflow (Inbox → Today → Upcoming → Done) (Priority: P1)

**Goal**: All tasks flow through 4 fixed, non-customizable views. New tasks land in Inbox. Users manually promote to Today. Upcoming shows future tasks. Done is archive. Navigation bar always shows all 4 views.

**Independent Test**: New task lands in Inbox, moving task to Today removes from Inbox, Upcoming sorts by due date, Done view shows completed tasks in reverse chrono order, all views accessible via bottom nav, no custom filters possible

### Tests for User Story 4

- [x] T042 [P] [US4] Create test for new task creation landing in Inbox by default in `QuickAddViewModelTest.kt`
- [x] T043 [P] [US4] Create `TaskRepositoryTest.kt` in `app/src/test/java/com/vega/data/repository/` using in-memory Room database: getInboxTasks returns only INBOX tasks, getTodayTasks returns only TODAY, getUpcomingTasks returns UPCOMING sorted by dueDate ASC, getDoneTasks returns DONE sorted by updatedAt DESC

### Implementation for User Story 4

- [x] T044 Create `InboxViewModel` in `app/src/main/java/com/vega/ui/viewmodels/InboxViewModel.kt` exposing StateFlow<List<Task>> from repository.getInboxTasks(), methods: moveToToday(), moveToUpcoming(), completeTask(), deleteTask()
- [x] T045 [P] Create `UpcomingViewModel` in `app/src/main/java/com/vega/ui/viewmodels/UpcomingViewModel.kt` exposing tasks sorted by due date ASC, methods to move to Today or mark done
- [x] T046 [P] Create `DoneViewModel` in `app/src/main/java/com/vega/ui/viewmodels/DoneViewModel.kt` exposing completed tasks sorted by updatedAt DESC, archive view (tasks can remain viewable but not edited in V1)
- [x] T047 Create `InboxFragment.kt`, `UpcomingFragment.kt`, `DoneFragment.kt` in `app/src/main/java/com/vega/ui/fragments/` each with RecyclerView displaying their respective task lists
- [x] T048 Create layouts for each: `app/src/main/res/layout/fragment_inbox.xml`, `fragment_upcoming.xml`, `fragment_done.xml` with RecyclerView and empty state views
- [x] T049 Integrate Navigation Component in MainActivity: NavHostFragment with navigation graph, BottomNavigationView linked via NavigationUI.setupWithNavController, bottom nav always visible across all fragments
- [x] T050 Verify workflow enforcement: update TaskRepository.createTask to always set state = INBOX for new tasks, verify no other default paths bypass this
- [x] T051 [P] Disable custom view creation: hide any filter/sort options from UI in V1 (deferred to future versions per constitution 4.0)

**Checkpoint**: User Story 4 complete — Fixed workflow established, all 4 views functional and navigable

---

## Phase 7: User Story 5 — End-of-Day Snooze Ritual (Priority: P2)

**Goal**: At 21:00 daily, WorkManager triggers query for unfinished Today tasks. For each, display bottom sheet with 4 options: move to tomorrow, keep in Today, mark done, delete. Ritual should feel lightweight.

**Independent Test**: WorkManager triggers at correct time, one sheet per unfinished task (sequential), 4 options functional, ritual completes in <30 seconds for 2 tasks, Today view reflects changes

### Tests for User Story 5

- [x] T052 [P] [US5] Create `EndOfDaySnoozeWorkerTest.kt` testing WorkManager integration: worker queries unfinished Today tasks, posts notifications for each, applies user choices to database

### Implementation for User Story 5

- [x] T053 Create `EndOfDaySnoozeWorker` in `app/src/main/java/com/vega/workers/EndOfDaySnoozeWorker.kt`: queries repository for TODAY tasks, calls notificationManager.postNotification for each, passes task ID to notification intent
- [x] T054 Setup WorkManager in AppModule (Hilt): create PeriodicWorkRequest scheduled for 21:00 daily, enqueue in VegaApplication.onCreate()
- [x] T055 Create snooze bottom sheet `SnoozeBottomSheetFragment.kt` in `app/src/main/java/com/vega/ui/fragments/` receiving task ID intent: displays 4 buttons with icons/labels (move to tomorrow, keep in Today, mark as done, delete), calls repository method based on selection, dismisses and shows next sheet (activity manages queue)
- [x] T056 Create snooze sheet layout `app/src/main/res/layout/bottom_sheet_snooze.xml` with: task title at top, 4 MaterialButtons in grid/list, task metadata (due date, priority if set)
- [x] T057 Create notification setup: `NotificationCompat.Builder` with channel `vega_reminders` (importance HIGH), title "Review unfinished tasks", each task ID as separate notification with task title
- [x] T058 Integrate snooze flow: when notification tapped, open activity that queues snooze sheets for all unfinished tasks, dismissing one shows next, final dismissal ends ritual and refreshes Today view
- [x] T059 [P] Create settings toggle for end-of-day reminders in SettingsFragment (optional, deferred if time-constrained)

**Checkpoint**: User Story 5 complete — End-of-day ritual guides daily reset

---

## Phase 8: User Story 6 — Swipe Gestures for Task Actions (Priority: P2)

**Goal**: Swipe right marks task done. Swipe left reveals postpone + delete. Long press opens detail/edit sheet. All actions <200ms response time.

**Independent Test**: Swipe right instantly moves task to Done, swipe left reveals action buttons, long press opens edit sheet, all response <200ms, changes saved immediately

### Tests for User Story 6

- [X] T060 [P] [US6] Test swipe gesture response timing and correctness (can be manual UI test in V2, deferred for now but task documented)

### Implementation for User Story 6

- [X] T061 Create `TaskListAdapter` in `app/src/main/java/com/vega/ui/adapters/TaskListAdapter.kt` extending RecyclerView.Adapter<TaskViewHolder> with ItemTouchHelper callback methods for swipe left/right
- [X] T062 Create `TaskViewHolder` in adapter: renders task in `item_task.xml` layout, handles click/long-press events
- [X] T063 Implement ItemTouchHelper.Callback in adapter: onSwiped(viewHolder, direction) → direction RIGHT calls completeTask(), direction LEFT reveals action buttons
- [X] T064 Create swipe right animation: task slides right, fades, removed from list, update ViewModel, task moves to Done view instantly
- [X] T065 Create swipe left action reveal: task does not slide, contextual action buttons appear (postpone with calendar icon, delete with trash icon), semi-transparent background
- [X] T066 Implement postpone action: moves task to tomorrow or Upcoming depending on current view, updates due date accordingly
- [X] T067 Implement delete action: shows optional confirmation dialog ("Delete this task?"), removes task from database if confirmed
- [X] T068 Implement long press: calls FragmentManager.beginTransaction() to show TaskDetailFragment in bottom sheet with all 5 fields editable
- [X] T069 Create `TaskDetailFragment` as BottomSheetDialogFragment in `app/src/main/java/com/vega/ui/fragments/TaskDetailFragment.kt`: displays TextInputLayout for title, due date picker, priority dropdown, state spinner, notes TextInputEditText, save button
- [X] T070 Create detail layout `app/src/main/res/layout/bottom_sheet_task_detail.xml`: 5 input fields corresponding to Task entity, save/cancel buttons
- [X] T071 Detail sheet save: updates task in repository, dismisses sheet, list refreshes automatically via Flow subscription

**Checkpoint**: User Story 6 complete — Touch gestures enable frictionless task management

---

## Phase 9: User Story 7 — Global Search (Priority: P2)

**Goal**: Top bar search icon opens search view. User types keyword. Results show all matching tasks across all views with state label. Tapping result jumps to task in its view.

**Independent Test**: Search accessible from all screens, results returned in <500ms for 1000+ tasks, state labels shown, tapping result navigates correctly, search keyboard responsive

### Tests for User Story 7

- [X] T072 [P] [US7] Create search query test in `TaskRepositoryTest.kt`: repository.searchByTitle(query) returns matching tasks, search works across all states

### Implementation for User Story 7

- [X] T073 Add search icon in top bar of MainActivity: toolbar with search action item launching SearchFragment
- [X] T074 Create `SearchFragment` in `app/src/main/java/com/vega/ui/fragments/SearchFragment.kt` with: search bar (SearchView or TextInputEditText), RecyclerView for results, empty state ("No tasks found")
- [X] T075 Create `SearchViewModel` in `app/src/main/java/com/vega/ui/viewmodels/SearchViewModel.kt`: takes query string, calls repository.searchByTitle(query), returns StateFlow<List<Task>>
- [X] T076 Create search layout `app/src/main/res/layout/fragment_search.xml` with: MaterialAppBarLayout with search field at top, RecyclerView for results below
- [X] T077 Create search result item layout `app/src/main/res/layout/item_search_result.xml`: task title, state badge (Inbox, Today, Upcoming, Done colors), priority indicator, quick action buttons
- [X] T078 Implement result click handler: tapping result calls navigation to appropriate view (Inbox, Today, Upcoming, Done) and scrolls to task, or opens task detail sheet
- [X] T079 [P] Optimize search: debounce keyboard input (wait 300ms after user stops typing before querying), show loading state while results load

**Checkpoint**: User Story 7 complete — Users can find tasks across all views

---

## Phase 10: User Story 8 — Gentle Reminders (Priority: P3)

**Goal**: Two notification types only: (1) "You still have tasks in Today" once at 6pm if unfinished, (2) Weekly review prompt Sunday evening. Both optional, can be disabled in Settings.

**Independent Test**: Daily notification fires once at 6pm if Today has tasks, weekly prompt fires Sunday evening, notifications respect user preference toggle, only 2 notification types exist

### Tests for User Story 8

- [x] T080 [P] [US8] Test notification scheduling logic (manual verification in V2, documented for now)

### Implementation for User Story 8

- [x] T081 Create second WorkManager PeriodicWorkRequest for daily 6pm reminder in AppModule: queries Today for unfinished tasks, if count > 0, posts notification "You still have [N] tasks in Today"
- [x] T082 Create third WorkManager PeriodicWorkRequest for weekly Sunday 6pm review prompt: posts notification "Review your Inbox and plan your week"
- [x] T083 Create Settings screen `SettingsFragment` in `app/src/main/java/com/vega/ui/fragments/SettingsFragment.kt` with: toggles for "End-of-day reminders", "Daily reminders", "Weekly review prompt", saves to SharedPreferences
- [x] T084 Update workers to respect preferences: check SharedPreferences for enabled status before posting notifications
- [x] T085 [P] Create notification channel management in AppModule or VegaApplication: single channel `vega_reminders` with HIGH importance, set description "Task reminders"

**Checkpoint**: User Story 8 complete — Gentle reminders reinforce habit without intrusiveness

---

## Phase 11: User Story 9 — Theme Support (Light/Dark) (Priority: P3)

**Goal**: App supports light and dark themes. Defaults to system preference. User can override in Settings. All Material 2 components adapt automatically.

**Independent Test**: App renders in light and dark correctly, respects system preference on startup, theme toggle in Settings applies immediately, text readable in both with WCAG AA contrast

### Tests for User Story 9

- [x] T086 [P] [US9] Theme support is UI-only, testing deferred to V2 (manual verification)

### Implementation for User Story 9

- [x] T087 Verify theme XML already created in Phase 2 (T013): `values/themes.xml` and `values-night/colors.xml` configured for Material Design 2 DayNight support
- [x] T088 Create `ThemeManager` in `app/src/main/java/com/vega/ui/theme/ThemeManager.kt`: provides current theme preference (LIGHT, DARK, SYSTEM), methods to set preference, saves to SharedPreferences
- [x] T089 Integrate theme toggle in SettingsFragment: dropdown with options "System (default)", "Light", "Dark", saves selection via ThemeManager
- [x] T090 Apply theme at app startup: VegaApplication.onCreate() calls ThemeManager.applyTheme() before activities launch
- [x] T091 Apply theme at runtime: when user changes preference, emit LocalBroadcast or use EventBus to recreate current activity with new theme (androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode)
- [x] T092 Verify contrast: test all screens in both themes, ensure text contrast meets WCAG 2.1 AA (4.5:1 for normal text, 3:1 for large text)
- [x] T093 [P] Create accent colors and component overrides in both light/dark color files to ensure consistency

**Checkpoint**: User Story 9 complete — App is accessible in both light and dark modes

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Improvements affecting multiple stories, documentation, and quality assurance

- [ ] T094 Natural language parser edge case refinement: verify all 20+ test cases pass, refine parsing logic for real-world inputs
- [ ] T095 Performance optimization: profile app launch time (target <1 second), optimize Room queries (ensure indexes on state, due_date, created_at), verify swipe response <200ms
- [ ] T096 Offline functionality validation: verify app works completely offline (disable network, test all operations), verify data persists across restarts
- [ ] T097 Error handling and validation: add try-catch blocks around database operations, display user-friendly error messages (snackbars) for failures, log errors for debugging
- [ ] T098 [P] Run quickstart.md validation scenarios: manually execute all 8 scenarios from quickstart.md, verify acceptance criteria pass
- [ ] T099 [P] Unit test execution: ensure all unit tests in `app/src/test/` pass (parser, ViewModels, repository with in-memory DB, scoring function)
- [ ] T100 Code cleanup and refactoring: remove unused imports, consistent naming conventions, extract magic strings to resources, add JavaDoc comments
- [ ] T101 [P] Documentation: create README.md with build instructions, add inline code comments for complex logic (parser, scoring), document assumptions
- [ ] T102 Security review: verify no sensitive data in logs, check permission usage (RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM justified), verify data isolation
- [ ] T103 Accessibility review: verify all interactive elements are accessible via keyboard, text sizes readable, high contrast in both themes
- [ ] T104 ProGuard/R8 configuration: enable for release builds, test obfuscation does not break functionality, measure APK size reduction
- [ ] T105 Final quality gate: no ANRs, crashes, or data loss observed, all features work on Android 8.0 (minimum), all success criteria from spec.md met

**Checkpoint**: All user stories functional, tested, and polished

---

## Dependencies & Execution Order

### Phase Dependencies

| Phase | Depends On | Status |
|-------|------------|--------|
| Setup (Phase 1) | Nothing | Start immediately |
| Foundational (Phase 2) | Setup | Can start after Phase 1 |
| User Stories 1–4 (P1) | Foundational | Can start after Phase 2, proceed in parallel or sequence |
| User Stories 5–7 (P2) | Foundational + P1 | Can start after P1 stories (or in parallel if staffed) |
| User Stories 8–9 (P3) | Foundational + P2 | Can start after P2 stories (or in parallel if staffed) |
| Polish (Phase 12) | All user stories | Start after all desired stories complete |

### Within Each User Story

1. **Tests first** (if included): Write tests, verify they FAIL
2. **Implementation**: Build features to make tests PASS
3. **Integration**: Ensure story works independently
4. **Quality**: Code review, refactoring, documentation

### Parallel Opportunities

**Phase 1 Setup**:
- T003, T004, T011, T012, T014, T015 can run in parallel (different files, no dependencies)

**Phase 2 Foundational**:
- T011, T012, T014, T015 can run in parallel
- T007–T010 are sequential (database, DAO, DB class, repository in dependency order)
- After database setup, T013–T019 (theme, layout, navigation) can run in parallel

**Phase 3–11 User Stories**:
- P1 stories can run in parallel after Foundational (if team size allows)
- P2 stories can start once P1 complete
- P3 stories can start once P2 complete
- Within each story: test + models can run in parallel, then services, then UI

**Phase 12 Polish**:
- T094–T105: Most tasks independent, can parallelize (except final quality gate which is sequential)

---

## Parallel Example: User Story 1 Implementation

```text
Parallel Batch 1 (Tests):
- T020: Parser test suite (20+ cases)
- T021: QuickAddViewModel test

Parallel Batch 2 (Implementation after tests fail):
- T022: NaturalLanguageParser logic
- T023: ParseResult data class

Parallel Batch 3 (UI):
- T024: QuickAddViewModel
- T025: QuickAddBottomSheetFragment
- T026: Layout XML

Batch 4 (Integration):
- T027: Integrate FAB in MainActivity
- T028: Validation logic
```

All could run in parallel on separate branches, merged as they complete.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. ✅ Phase 1: Setup (1 day)
2. ✅ Phase 2: Foundational (2–3 days)
3. ✅ Phase 3: User Story 1 (1–2 days)
4. **STOP and VALIDATE**: Verify task capture works end-to-end
5. **DEMO**: Show working task capture to stakeholders
6. Proceed to P1 stories 2–4

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (Quick Capture) → Test independently → **Release MVP** ✨
3. Add US2 (Today View) → Enhance MVP
4. Add US3 (Next Best Action) → Intelligence layer added
5. Add US4 (Fixed Workflow) → Complete workflow
6. Add P2 stories (End-of-day, Swipe, Search) → Feature-rich
7. Add P3 stories (Reminders, Theme) → Polish

### Parallel Team Strategy (if 3+ developers)

```text
Team size 3:
- Dev 1: Foundational setup + US1 (Quick Capture)
- Dev 2: US2 (Today View) + US3 (Next Best Action)
- Dev 3: US4 (Fixed Workflow) + US5 (End-of-Day Snooze)

After P1 complete:
- Dev 1: US5 remaining + US6 (Swipe Gestures)
- Dev 2: US7 (Search)
- Dev 3: US8 (Reminders) + US9 (Theme)
```

---

## Notes

- **[P]** = Parallelizable (different files, no blocking dependencies)
- **[Story]** = Task belongs to specific user story (US1–US9)
- Each story is independently deployable and testable
- Commit after task or logical group (T001–T006, T007–T010, etc.)
- Stop at any checkpoint (after each user story) to validate independently
- All tests must FAIL before implementing, then PASS after
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence

---

## Acceptance Criteria

Vega is acceptable when:

✅ All Phase 1 + Phase 2 tasks complete (setup + foundation)  
✅ At least US1–US4 (P1) complete and tested independently  
✅ All unit tests pass (parser, ViewModels, repository, scoring)  
✅ All 8 quickstart.md scenarios execute successfully  
✅ App launches in <1 second on mid-range device  
✅ Swipe responses <200ms  
✅ Parser handles 95%+ of natural language inputs  
✅ No ANRs, crashes, or data loss  
✅ Offline mode works without network  
✅ Theme support functional  
✅ All success criteria from spec.md met  

---

**Total Task Count**: 105 tasks  
**P1 (MVP Foundation)**: Setup (6) + Foundational (13) + US1-4 (78) = **97 tasks**  
**P2**: US5-7 (18 additional tasks) = **18 tasks**  
**P3**: US8-9 (12 additional tasks) = **12 tasks**  
**Polish**: Phase 12 = **12 tasks**  

**MVP Scope Recommendation**: Complete Phase 1 + Phase 2 + User Stories 1–4 (all P1 stories) for minimum viable product. This delivers:
- Quick task capture with natural language
- Focused Today view (cognitive load cap)
- Intelligent Next Best Action recommendation
- Fixed 4-view workflow
- All core features users need to "decide what to do next"

**Estimated Timeline**: 
- 1 developer: 3–4 weeks
- 2 developers (parallelizing P1 + P2): 2–3 weeks
- 3 developers (full parallelization): 1.5–2 weeks

---

**Plan Status**: ✅ Ready for implementation

**Date**: 2026-06-04  
**Version**: Tasks 1.0.0
