package io.github.androidpoet.kdb.client

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.flatMap
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.paging.KdbPagingEngine
import io.github.androidpoet.kdb.query.KdbQueryEngine
import io.github.androidpoet.kdb.schema.MigrationRunner
import io.github.androidpoet.kdb.schema.SchemaDefinition

public class KdbClient(
    public val driver: KdbDriver,
    public val schema: SchemaDefinition
) {
    public val query: KdbQueryEngine = KdbQueryEngine(driver)
    public val paging: KdbPagingEngine = KdbPagingEngine(driver, query)
    private val migrationRunner = MigrationRunner(driver)

    public fun open(): KdbResult<Unit> {
        return driver.open().flatMap {
            migrationRunner.migrate(schema.migrations)
        }
    }

    public fun close(): KdbResult<Unit> {
        return driver.close()
    }
}

public class KdbConfig {
    public var name: String = "kdb.db"
    public var schema: SchemaDefinition = SchemaDefinition(emptyList())
}

public fun createKdb(driver: KdbDriver, block: KdbConfig.() -> Unit = {}): KdbClient {
    val config = KdbConfig().apply(block)
    return KdbClient(driver, config.schema)
}
