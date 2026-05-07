# KDB: Kotlin Multiplatform SQLite Wrapper

<p align="center">
  <img src="art/kdb_logo.png" alt="KDB Logo" width="200"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.10-blue?logo=kotlin" alt="Kotlin Version"/>
  <img src="https://img.shields.io/badge/Platform-Multiplatform-orange" alt="Platform Support"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

**KDB** is a lightweight, type-safe SQLite wrapper built for Kotlin Multiplatform.

## Core API (Simple by design)

1. `open()`
2. `migrate(vararg Migration)`
3. `insert(item)`
4. `updateById(id, item)`
5. `deleteById(id)`
6. `getById(id)`
7. `list(limit, afterId, idSelector)`
8. `driver.transaction { ... }`

## Modules

| Module | Description |
| :--- | :--- |
| `kdb` | Core library with driver, schema, query, paging, and client API |
| `kdb-paging3` | Optional Paging 3 integration |

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

driver.transaction {
    kdb.insert(Task(2, "A", false)).getOrThrow()
    kdb.insert(Task(3, "B", false)).getOrThrow()
}
```

## License

KDB is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.
