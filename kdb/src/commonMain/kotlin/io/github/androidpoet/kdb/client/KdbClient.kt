package io.github.androidpoet.kdb.client

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.paging.KdbPagingEngine
import io.github.androidpoet.kdb.paging.Page
import io.github.androidpoet.kdb.query.AutoTable
import io.github.androidpoet.kdb.query.KdbQueryEngine
import io.github.androidpoet.kdb.query.Table
import io.github.androidpoet.kdb.schema.Migration
import io.github.androidpoet.kdb.schema.MigrationRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public class KdbClient(
    public val driver: KdbDriver,
    private val config: KdbConfig,
) {
    public val query: KdbQueryEngine = KdbQueryEngine(driver)
    public val paging: KdbPagingEngine = KdbPagingEngine(query)
    private val migrationRunner = MigrationRunner(driver)

    @PublishedApi
    internal val tables: MutableMap<String, Table<*>> = mutableMapOf()

    init {
        config.entities.forEach { serializer ->
            val name = AutoTable.inferTableName(serializer.descriptor)
            tables[serializer.descriptor.serialName] = AutoTable(name, serializer as KSerializer<Any>)
        }
    }

    public val dispatcher: CoroutineDispatcher get() = config.dispatcher

    public fun open(): KdbResult<Unit> = driver.open()

    public fun migrate(vararg migrations: Migration): KdbResult<Unit> {
        return migrationRunner.migrate(migrations.toList())
    }

    public fun close(): KdbResult<Unit> = driver.close()

    @Suppress("UNCHECKED_CAST")
    public inline fun <reified T : Any> getTable(): Table<T> {
        val serialName = serializer<T>().descriptor.serialName
        return tables.getOrPut(serialName) {
            AutoTable(AutoTable.inferTableName(serializer<T>().descriptor), serializer<T>())
        } as Table<T>
    }

    public inline fun <reified T : Any> insert(item: T): KdbResult<Unit> {
        return query.insert(getTable<T>(), item)
    }

    public inline fun <reified T : Any> updateById(id: Long, item: T): KdbResult<Unit> {
        return query.updateById(getTable<T>(), item, id)
    }

    public inline fun <reified T : Any> deleteById(id: Long): KdbResult<Unit> {
        return query.deleteById(getTable<T>(), id)
    }

    public inline fun <reified T : Any> getById(id: Long): KdbResult<T?> {
        return query.getById(getTable<T>(), id)
    }

    public inline fun <reified T : Any> list(
        limit: Int = 20,
        afterId: Long? = null,
        noinline idSelector: (T) -> Long,
    ): KdbResult<Page<T>> {
        return paging.list(getTable<T>(), limit, afterId, idSelector)
    }

    public suspend fun openOrThrow(): Unit = withContext(dispatcher) { open().getOrThrow() }

    public suspend fun migrateOrThrow(vararg migrations: Migration): Unit = withContext(dispatcher) {
        migrate(*migrations).getOrThrow()
    }

    public suspend fun closeOrThrow(): Unit = withContext(dispatcher) { close().getOrThrow() }

    public suspend inline fun <reified T : Any> insertOrThrow(item: T): Unit = withContext(dispatcher) {
        insert(item).getOrThrow()
    }

    public suspend inline fun <reified T : Any> updateByIdOrThrow(id: Long, item: T): Unit = withContext(dispatcher) {
        updateById(id, item).getOrThrow()
    }

    public suspend inline fun <reified T : Any> deleteByIdOrThrow(id: Long): Unit = withContext(dispatcher) {
        deleteById<T>(id).getOrThrow()
    }

    public suspend inline fun <reified T : Any> getByIdOrThrow(id: Long): T? = withContext(dispatcher) {
        getById<T>(id).getOrThrow()
    }

    public suspend inline fun <reified T : Any> listOrThrow(
        limit: Int = 20,
        afterId: Long? = null,
        noinline idSelector: (T) -> Long,
    ): Page<T> = withContext(dispatcher) {
        list(limit, afterId, idSelector).getOrThrow()
    }

    public suspend fun <T> tx(block: KdbClient.() -> T): T = withContext(dispatcher) {
        driver.transaction { this@KdbClient.block() }.getOrThrow()
    }
}

public class KdbConfig {
    public var name: String = "kdb.db"
    public var dispatcher: CoroutineDispatcher = Dispatchers.Default
    public val entities: MutableList<KSerializer<out Any>> = mutableListOf()

    public fun entities(vararg serializers: KSerializer<out Any>) {
        entities.addAll(serializers)
    }
}

public fun createKdb(driver: KdbDriver, block: KdbConfig.() -> Unit = {}): KdbClient {
    val config = KdbConfig().apply(block)
    return KdbClient(driver, config)
}
