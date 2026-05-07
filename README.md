# KDB: Kotlin Multiplatform SQLite Wrapper

<p align="center">
  <img src="art/kdb_logo.png" alt="KDB Logo" width="200"/>
</p>

KDB is a lightweight, type-safe SQLite wrapper for Kotlin Multiplatform.

## Modules

| Module | Description |
| :--- | :--- |
| `kdb` | Core library (driver + schema + query + paging + client API) |
| `kdb-paging3` | Optional Paging 3 integration |

## API Design

- Primary API returns `KdbResult<T>`:
  - `open`, `migrate`, `close`
  - `insert`, `updateById`, `deleteById`, `getById`, `list`
- Convenience `suspend` background APIs throw on failure:
  - `openOrThrow`, `migrateOrThrow`, `closeOrThrow`
  - `insertOrThrow`, `updateByIdOrThrow`, `deleteByIdOrThrow`, `getByIdOrThrow`, `listOrThrow`
- Transaction helper:
  - `suspend fun tx { ... }`

## Configuration

```kotlin
val kdb = createKdb(driver) {
    entities(Task.serializer())
    // optional, default: Dispatchers.Default
    // dispatcher = Dispatchers.IO
}
```

## Quick Start (Result-first)

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
```

## Convenience (Suspend + Throwing)

```kotlin
kdb.openOrThrow()
kdb.insertOrThrow(Task(2, "A", false))

val page = kdb.listOrThrow<Task>(limit = 20) { it.id }

kdb.tx {
    insert(Task(3, "B", false)).getOrThrow()
    insert(Task(4, "C", false)).getOrThrow()
}
```

## License

KDB is licensed under the MIT License. See [LICENSE](LICENSE).
