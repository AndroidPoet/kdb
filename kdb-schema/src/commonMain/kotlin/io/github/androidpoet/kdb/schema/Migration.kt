package io.github.androidpoet.kdb.schema

import io.github.androidpoet.kdb.core.KdbError
import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.flatMap
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.driver.KdbCursor

public data class Migration(
    val version: Int,
    val upSql: List<String>
)

public class MigrationRunner(private val driver: KdbDriver) {
    public fun migrate(migrations: List<Migration>): KdbResult<Unit> {
        return driver.transaction {
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
    }

    private fun ensureMetadataTable() {
        driver.execute("CREATE TABLE IF NOT EXISTS kdb_metadata (key TEXT PRIMARY KEY, value TEXT)").getOrThrow()
    }

    private fun getCurrentVersion(): Int {
        val cursor = driver.prepare("SELECT value FROM kdb_metadata WHERE key = 'version'")
            .flatMap { it.executeQuery() }
            .getOrThrow()
        
        return if (cursor.next()) {
            val version = cursor.getText(0).toInt()
            cursor.close()
            version
        } else {
            cursor.close()
            0
        }
    }

    private fun updateVersion(version: Int) {
        driver.execute("INSERT OR REPLACE INTO kdb_metadata (key, value) VALUES ('version', '$version')").getOrThrow()
    }
}
