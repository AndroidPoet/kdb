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

### 1. Define your Entity

```kotlin
@Serializable
data class Task(val id: Long, val title: String, val isDone: Boolean)
```

### 2. Initialize KDB (Zero-SQL Auto-Schema!)

```kotlin
val driver = CommonKdbDriver("my_app.db")

val kdb = createKdb(driver) {
    // KDB will automatically generate the schema and run CREATE TABLE 
    // for all registered entities when you open the database!
    entities(Task.serializer())
}

kdb.open().onFailure { println("Database error: ${it.message}") }
```

### 3. Fluent CRUD Operations

```kotlin
val task = Task(1, "Build KDB", false)

// Insert (Table inferred automatically!)
kdb.insert(task)
    .onSuccess { println("Task saved!") }

// Query All
val tasks = kdb.selectAll<Task>().getOrElse { emptyList() }

// Delete
kdb.delete<Task>("id = 1")
```

### 4. Optional: Paging 3 Integration

```kotlin
// Get the auto-generated table reference for advanced usage
val taskTable = kdb.getTable<Task>()
val pagingSource = KdbPagingSource(kdb.paging, taskTable)
```

## 📄 License

KDB is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---
<p align="center">
  Built with ❤️ for the Kotlin Multiplatform ecosystem by <a href="https://github.com/AndroidPoet">AndroidPoet</a>.
</p>
