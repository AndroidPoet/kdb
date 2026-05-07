package io.github.androidpoet.kdb.client

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.flatMap
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.paging.KdbPagingEngine
import io.github.androidpoet.kdb.query.AutoTable
import io.github.androidpoet.kdb.query.KdbQueryEngine
import io.github.androidpoet.kdb.query.Table
import io.github.androidpoet.kdb.schema.MigrationRunner
import io.github.androidpoet.kdb.schema.SchemaDefinition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public class KdbClient(
    public val driver: KdbDriver,
    private val config: KdbConfig
) {
    public val query: KdbQueryEngine = KdbQueryEngine(driver)
    public val paging: KdbPagingEngine = KdbPagingEngine(driver, query)
    private val migrationRunner = MigrationRunner(driver)
    
    @PublishedApi
    internal val tables: MutableMap<String, Table<*>> = mutableMapOf()

    init {
        // Pre-register tables from config
        config.entities.forEach { serializer ->
            val name = AutoTable.inferTableName(serializer.descriptor)
            tables[serializer.descriptor.serialName] = AutoTable(name, serializer as KSerializer<Any>)
        }
    }

    public fun open(): KdbResult<Unit> {
        return driver.open().flatMap {
            // Run manual migrations if any
            migrationRunner.migrate(config.schema.migrations)
        }.flatMap {
            // Run auto-schema creation for registered entities
            var result: KdbResult<Unit> = KdbResult.success(Unit)
            config.entities.forEach { serializer ->
                val sql = AutoTable.generateCreateTableSql(serializer)
                result = result.flatMap { driver.execute(sql) }
            }
            result
        }
    }

    public fun close(): KdbResult<Unit> {
        return driver.close()
    }

    @Suppress("UNCHECKED_CAST")
    public inline fun <reified T : Any> getTable(): Table<T> {
        val serialName = serializer<T>().descriptor.serialName
        return tables.getOrPut(serialName) {
            AutoTable(AutoTable.inferTableName(serializer<T>().descriptor), serializer<T>())
        } as Table<T>
    }

    // --- High-Level DX CRUD Methods ---

    public inline fun <reified T : Any> insert(item: T): KdbResult<Unit> {
        return query.insert(getTable<T>(), item)
    }

    public inline fun <reified T : Any> selectAll(): KdbResult<List<T>> {
        return query.selectAll(getTable<T>())
    }

    public inline fun <reified T : Any> delete(whereClause: String): KdbResult<Unit> {
        return driver.execute("DELETE FROM ${getTable<T>().tableName} WHERE $whereClause")
    }
}

public class KdbConfig {
    public var name: String = "kdb.db"
    public var schema: SchemaDefinition = SchemaDefinition(emptyList())
    public val entities: MutableList<KSerializer<out Any>> = mutableListOf()

    public fun entities(vararg serializers: KSerializer<out Any>) {
        entities.addAll(serializers)
    }
}

public fun createKdb(driver: KdbDriver, block: KdbConfig.() -> Unit = {}): KdbClient {
    val config = KdbConfig().apply(block)
    return KdbClient(driver, config)
}
