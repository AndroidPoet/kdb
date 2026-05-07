package io.github.androidpoet.kdb.schema

import io.github.androidpoet.kdb.core.KdbError
import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.SqlValue
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.query.AutoTable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public data class Migration(
    val version: Int,
    val upSql: List<String>,
)

public class MigrationBuilder internal constructor() {
    private val statements: MutableList<String> = mutableListOf()

    public fun sql(statement: String) {
        statements.add(statement)
    }

    public inline fun <reified T : Any> createTable(tableName: String? = null) {
        createTable(serializer<T>(), tableName)
    }

    public fun createTable(serializer: KSerializer<*>, tableName: String? = null) {
        statements.add(AutoTable.generateCreateTableSql(serializer, tableName))
    }

    internal fun build(version: Int): Migration = Migration(version = version, upSql = statements.toList())
}

public fun migration(version: Int, block: MigrationBuilder.() -> Unit): Migration {
    val builder = MigrationBuilder()
    builder.block()
    return builder.build(version)
}

public class MigrationRunner(private val driver: KdbDriver) {
    public fun migrate(migrations: List<Migration>): KdbResult<Unit> {
        val result = driver.transaction {
            ensureMetadataTable()
            val currentVersion = getCurrentVersion()
            migrations.filter { it.version > currentVersion }
                .sortedBy { it.version }
                .forEach { migration ->
                    migration.upSql.forEach { sql ->
                        driver.execute(sql).getOrThrow()
                    }
                    updateVersion(migration.version)
                }
        }

        return when (result) {
            is KdbResult.Success -> KdbResult.success(Unit)
            is KdbResult.Failure -> KdbResult.failure(
                KdbError.MigrationError("Failed to run migrations: ${result.error.message}", result.error.cause),
            )
        }
    }

    private fun ensureMetadataTable() {
        driver.execute("CREATE TABLE IF NOT EXISTS kdb_metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            .getOrThrow()
    }

    private fun getCurrentVersion(): Int {
        val stmt = driver.prepare("SELECT value FROM kdb_metadata WHERE key = ?").getOrThrow()
        stmt.bindText(1, "version")
        val cursor = stmt.executeQuery().getOrThrow()

        val version = if (cursor.next()) cursor.getText(0).toIntOrNull() ?: 0 else 0
        cursor.close()
        stmt.close()
        return version
    }

    private fun updateVersion(version: Int) {
        driver.execute(
            sql = "INSERT OR REPLACE INTO kdb_metadata (key, value) VALUES (?, ?)",
            args = listOf(SqlValue.TextValue("version"), SqlValue.TextValue(version.toString())),
        ).getOrThrow()
    }
}
