# Data Model: Vega — Minimalist Personal Task Manager

**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)  
**Phase**: 1 (Design)

---

## Entity Definitions

### Task (Core Entity)

The atomic unit of work in Vega. Represents a single action or deliverable the user needs to complete.

```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Required fields
    val title: String,                           // User-provided text describing the task
    
    // Optional fields
    val dueDate: Long? = null,                   // Epoch milliseconds; nullable
    val priority: TaskPriority = TaskPriority.NONE,   // NONE, LOW, MEDIUM, HIGH
    val notes: String? = null,                   // Freeform user notes
    
    // State management
    val state: TaskState,                        // INBOX, TODAY, UPCOMING, DONE
    
    // Audit fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**Fields**:

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | String (UUID) | Yes | No | Unique identifier; auto-generated |
| `title` | String | Yes | No | Task title (max 255 chars recommended) |
| `dueDate` | Long | No | Yes | Due date/time as epoch milliseconds |
| `priority` | TaskPriority | No | No | Priority level: NONE (default), LOW, MEDIUM, HIGH |
| `state` | TaskState | Yes | No | Current workflow state |
| `notes` | String | No | Yes | Optional freeform notes (max 2000 chars recommended) |
| `createdAt` | Long | Yes | No | Timestamp of creation |
| `updatedAt` | Long | Yes | No | Timestamp of last modification |

**Constraints**:

- `title` must not be empty (validated at ViewModel layer)
- `id` must be unique (enforced by Room @PrimaryKey)
- `dueDate` is stored as epoch milliseconds for timezone-agnostic comparisons
- `state` is never null (enforces workflow discipline)
- No cascading deletes; task deletion is explicit user action

---

## Enumerations

### TaskPriority

Represents the urgency/importance of a task.

```kotlin
enum class TaskPriority {
    NONE,      // Default; no explicit priority
    LOW,       // Nice to have, low urgency
    MEDIUM,    // Standard priority
    HIGH       // Urgent, high impact
}
```

**Mapping to UI**:
- `NONE`: No badge, standard text
- `LOW`: Light blue badge, icon
- `MEDIUM`: Orange badge, icon
- `HIGH`: Red badge, icon

**Storage**: Persisted as STRING in Room database

### TaskState

Represents the current position in the workflow. The four states are fixed and non-customizable per constitution principle 3.2.

```kotlin
enum class TaskState {
    INBOX,     // Entry point; unprocessed tasks
    TODAY,     // Tasks user is working on today
    UPCOMING,  // Tasks with future due dates
    DONE       // Completed tasks (archive)
}
```

**State Transitions** (allowed):
```
INBOX      → TODAY, UPCOMING, DONE, DELETE
TODAY      → UPCOMING, DONE, DELETE, (back to INBOX if moved)
UPCOMING   → TODAY, DONE, DELETE
DONE       → (archive; no transitions)
DELETE     → (terminal; not stored)
```

---

## Database Schema (Room)

### Tasks Table

```sql
CREATE TABLE tasks (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    due_date INTEGER,                    -- epoch milliseconds, nullable
    priority TEXT NOT NULL DEFAULT 'NONE', -- NONE, LOW, MEDIUM, HIGH
    state TEXT NOT NULL,                 -- INBOX, TODAY, UPCOMING, DONE
    notes TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Indexes for common queries
CREATE INDEX idx_tasks_state ON tasks(state);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `idx_tasks_state` | `state` | Frequent filtering by workflow state (Inbox, Today, Upcoming, Done) |
| `idx_tasks_due_date` | `due_date` | Sorting Upcoming tasks by due date |
| `idx_tasks_created_at` | `created_at` DESC | Ordering Done view chronologically |

**No foreign keys**: Flat schema per constitution 5.3; no relational joins in V1

---

## Relationships

**No relationships in V1**. Task is a standalone entity. Future expansions (subtasks, tags, projects) would introduce relationships but are deferred to V2.

---

## Computed/Derived Properties

### Next Best Action Score

Not persisted. Computed in `TodayViewModel` at query time.

```kotlin
fun scoreTask(task: Task, now: Long): Int {
    var score = 0
    
    // Overdue penalty (highest weight) — highest priority
    if (task.dueDate != null && task.dueDate < now) {
        score += 100
    }
    
    // Priority weight
    score += when (task.priority) {
        TaskPriority.HIGH -> 30
        TaskPriority.MEDIUM -> 15
        TaskPriority.LOW -> 5
        TaskPriority.NONE -> 0
    }
    
    // Due within next 2 hours
    if (task.dueDate != null && (task.dueDate - now) in 0..2.hours) {
        score += 40
    }
    
    // Due today
    if (task.dueDate != null && Calendar.getInstance().apply {
        timeInMillis = task.dueDate
    }.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
        score += 20
    }
    
    return score
}
```

**Usage**: Called on every Today view load. Highest-scoring task is emitted as `StateFlow<Task>` for Next Best Action card.

---

## Data Access Patterns

### TaskDao (Room)

```kotlin
@Dao
interface TaskDao {
    
    @Insert
    suspend fun insertTask(task: Task)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?
    
    @Query("SELECT * FROM tasks WHERE state = :state ORDER BY created_at DESC")
    fun getTasksByState(state: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE state = 'UPCOMING' ORDER BY due_date ASC")
    fun getUpcomingTasksSortedByDueDate(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE state = 'DONE' ORDER BY updated_at DESC")
    fun getDoneTasksSortedByRecency(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE :query")
    fun searchByTitle(query: String): Flow<List<Task>>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE state = :state")
    suspend fun countTasksByState(state: String): Int
}
```

### TaskRepository (Abstraction)

```kotlin
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    // Expose DAOs as Flow for reactive updates
    fun getInboxTasks(): Flow<List<Task>> = taskDao.getTasksByState("INBOX")
    fun getTodayTasks(): Flow<List<Task>> = taskDao.getTasksByState("TODAY")
    fun getUpcomingTasks(): Flow<List<Task>> = taskDao.getUpcomingTasksSortedByDueDate()
    fun getDoneTasks(): Flow<List<Task>> = taskDao.getDoneTasksSortedByRecency()
    
    // Write operations
    suspend fun createTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    
    // Search
    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchByTitle("%$query%")
}
```

---

## Data Lifecycle

### Task Creation (Quick Add)

1. User types text in Quick Add field
2. Natural Language Parser extracts title, dueDate, priority
3. ViewModel creates Task object with state = INBOX
4. Repository.createTask() inserts into Room
5. Optimistic insert → bottom sheet dismisses immediately
6. Inbox view updates via Flow subscription

### Task State Change

1. User swipes/long-presses task
2. ViewModel calls updateTask() with new state
3. Repository.updateTask() saves to Room
4. Flow-based view subscription triggers refresh
5. Task appears in new view

### Task Deletion

1. User swipes left, taps delete
2. ViewModel calls deleteTask()
3. Repository.deleteTask() removes from Room
4. View list updates immediately
5. No undo in V1 (future feature)

---

## Validation Rules

### Task Creation

| Field | Rule | Error Message |
|-------|------|---------------|
| `title` | Not empty, max 255 chars | "Task title cannot be empty" |
| `dueDate` | If present, must be valid epoch ms | "Invalid due date" |
| `priority` | Must be one of NONE/LOW/MEDIUM/HIGH | "Invalid priority" |
| `state` | Must be one of INBOX/TODAY/UPCOMING/DONE | "Invalid state" |
| `notes` | Optional, max 2000 chars | "Notes too long" |

### Task Update

All creation rules apply. Additional rule:
- State transition must be valid (enforced at ViewModel layer, not DB layer)

---

## Scaling & Performance Notes

**Current Design Supports**:
- 1000+ tasks without noticeable performance degradation
- Queries complete in <100ms on mid-range device
- Search results in <500ms for 1000+ tasks
- Room's in-memory caching + indexes handle typical query patterns

**Future Optimizations** (if needed):
- Pagination for Done view (currently all completed tasks load at once)
- Full-text search (FTS) if search performance becomes issue
- Task archiving (move old Done tasks to separate archive table)

---

**Status**: ✅ Data model finalized and validated against constitution

**Next**: Create quickstart.md with end-to-end validation scenarios
