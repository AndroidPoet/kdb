# KDB: Kotlin Multiplatform SQLite Wrapper

<p align="center">
  <img src="art/kdb_logo.png" alt="KDB Logo" width="200"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.10-blue?logo=kotlin" alt="Kotlin Version"/>
  <img src="https://img.shields.io/badge/Platform-Multiplatform-orange" alt="Platform Support"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

KDB is a lightweight, type-safe SQLite wrapper for Kotlin Multiplatform.

## Modules

| Module | Description |
| :--- | :--- |
| `kdb` | Core library (driver + schema + query + paging + client API) |
| `kdb-paging3` | Optional Paging 3 integration |

## API Style

KDB exposes two API styles:

1. `suspend` APIs (`open`, `migrate`, `insert`, `updateById`, `deleteById`, `getById`, `list`, `tx`) that execute on a configured background dispatcher.
2. `*Result` APIs (`openResult`, `migrateResult`, `insertResult`, etc.) that return `KdbResult<T>` for explicit error handling.

## Configuration

```kotlin
val kdb = createKdb(driver) {
    entities(Task.serializer())
    // optional, defaults to Dispatchers.Default
    // dispatcher = Dispatchers.IO
}
```

## Quick Start

```kotlin
@Serializable
data class Task(val id: Long, val title: String, val done: Boolean)

val driver = CommonKdbDriver("my_app.db")
val kdb = createKdb(driver) {
    entities(Task.serializer())
}

kdb.open()

kdb.migrate(
    Migration(
        version = 1,
        upSql = listOf(
            """
            CREATE TABLE IF NOT EXISTS task (
              id INTEGER PRIMARY KEY,
              title TEXT NOT NULL,
              done INTEGER NOT NULL
            )
            """.trimIndent()
        )
    )
)

kdb.insert(Task(1, "Ship KDB", false))
kdb.updateById(1, Task(1, "Ship KDB v1", false))
val one = kdb.getById<Task>(1)
kdb.deleteById<Task>(1)

val page = kdb.list<Task>(limit = 20, afterId = null) { it.id }

kdb.tx {
    insertResult(Task(2, "A", false)).getOrThrow()
    insertResult(Task(3, "B", false)).getOrThrow()
}
```

## Result API Example

```kotlin
val result = kdb.insertResult(Task(10, "Explicit Result", false))
result
    .onSuccess { println("Inserted") }
    .onFailure { println("Failed: ${it.message}") }
```

## License

KDB is licensed under the MIT License. See [LICENSE](LICENSE).
