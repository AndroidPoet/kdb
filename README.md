# KDB: Kotlin Multiplatform SQLite Wrapper

<p align="center">
  <img src="art/kdb_logo.png" alt="KDB Logo" width="200"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.10-blue?logo=kotlin" alt="Kotlin Version"/>
  <img src="https://img.shields.io/badge/Platform-Multiplatform-orange" alt="Platform Support"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

**KDB** is a lightweight, type-safe, and modular SQLite wrapper built specifically for **Kotlin Multiplatform**. It brings the ergonomics of a modern SDK to local SQLite development, allowing you to share your data layer across Android, iOS, JVM, and more.

## ✨ Features

- **🚀 Truly Multiplatform**: Powered by the official `androidx.sqlite` KMP library.
- **🛡️ Type-Safe CRUD**: Map objects to tables with zero reflection.
- **📜 Atomic Migrations**: Version-controlled schema management with automatic rollbacks.
- **💎 Functional Results**: Error handling inspired by `SupabaseResult` with `onSuccess`/`onFailure` chaining.
- **⏭️ Keyset Pagination**: High-performance paging that scales with your data.
- **🧩 Modular Design**: Only use what you need. Paging 3 is optional!

## 📦 Modules

| Module | Description |
| :--- | :--- |
| `kdb-core` | Result types, error hierarchy, and SQL primitives. |
| `kdb-driver` | Unified SQLite driver based on `BundledSQLiteDriver`. |
| `kdb-schema` | Schema definitions and migration runner. |
| `kdb-query` | The typed query engine for CRUD operations. |
| `kdb-paging` | Core keyset pagination logic. |
| `kdb-paging3` | **Optional** integration for Jetpack Paging 3 (KMP). |
| `kdb-client` | Ergonomic entrypoint and configuration DSL. |

## 🛠️ Installation

```kotlin
// commonMain
dependencies {
    implementation("io.github.androidpoet:kdb-client:0.1.0")
    // Optional: for Paging 3 support
    implementation("io.github.androidpoet:kdb-paging3:0.1.0")
}
```

## 📖 Quick Start

### 1. Define your Table

```kotlin
data class Task(val id: Long, val title: String, val isDone: Boolean)

object TaskTable : Table<Task> {
    override val tableName = "tasks"
    override val columns = listOf(
        Column<Long>("id", ColumnType.LONG),
        Column<String>("title", ColumnType.TEXT),
        Column<Boolean>("is_done", ColumnType.BOOLEAN)
    )

    override fun fromRow(cursor: KdbCursor) = Task(
        id = cursor.getLong(cursor.getColumnIndex("id")),
        title = cursor.getText(cursor.getColumnIndex("title")),
        isDone = cursor.getLong(cursor.getColumnIndex("is_done")) == 1L
    )

    override fun toValues(row: Task) = mapOf(
        "id" to SqlValue.LongValue(row.id),
        "title" to SqlValue.TextValue(row.title),
        "is_done" to SqlValue.BooleanValue(row.isDone)
    )
}
```

### 2. Initialize the Client

```kotlin
val driver = CommonKdbDriver("my_app.db")
val schema = SchemaDefinition(
    migrations = listOf(
        Migration(1, listOf("CREATE TABLE tasks (id INTEGER PRIMARY KEY, title TEXT, is_done INTEGER)"))
    )
)

val kdb = createKdb(driver) {
    this.schema = schema
}

// Open and run migrations
kdb.open().onFailure { println("Database error: ${it.message}") }
```

### 3. Fluent CRUD Operations

```kotlin
val task = Task(1, "Build KDB", false)

// Insert
kdb.query.insert(TaskTable, task)
    .onSuccess { println("Task saved!") }

// Query All
val tasks = kdb.query.selectAll(TaskTable).getOrElse { emptyList() }
```

### 4. Optional: Paging 3 Integration

```kotlin
val pagingSource = KdbPagingSource(kdb.paging, TaskTable)
val pager = Pager(PagingConfig(20)) { pagingSource }
```

## 📄 License

KDB is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---
<p align="center">
  Built with ❤️ for the Kotlin Multiplatform ecosystem by <a href="https://github.com/AndroidPoet">AndroidPoet</a>.
</p>
