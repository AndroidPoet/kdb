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

    public fun openResult(): KdbResult<Unit> = driver.open()

    public fun migrateResult(vararg migrations: Migration): KdbResult<Unit> {
        return migrationRunner.migrate(migrations.toList())
    }

    public fun closeResult(): KdbResult<Unit> = driver.close()

    @Suppress("UNCHECKED_CAST")
    public inline fun <reified T : Any> getTable(): Table<T> {
        val serialName = serializer<T>().descriptor.serialName
        return tables.getOrPut(serialName) {
            AutoTable(AutoTable.inferTableName(serializer<T>().descriptor), serializer<T>())
        } as Table<T>
    }

    public inline fun <reified T : Any> insertResult(item: T): KdbResult<Unit> {
        return query.insert(getTable<T>(), item)
    }

    public inline fun <reified T : Any> updateByIdResult(id: Long, item: T): KdbResult<Unit> {
        return query.updateById(getTable<T>(), item, id)
    }

    public inline fun <reified T : Any> deleteByIdResult(id: Long): KdbResult<Unit> {
        return query.deleteById(getTable<T>(), id)
    }

    public inline fun <reified T : Any> getByIdResult(id: Long): KdbResult<T?> {
        return query.getById(getTable<T>(), id)
    }

    public inline fun <reified T : Any> listResult(
        limit: Int = 20,
        afterId: Long? = null,
        noinline idSelector: (T) -> Long,
    ): KdbResult<Page<T>> {
        return paging.list(getTable<T>(), limit, afterId, idSelector)
    }

    public suspend fun open(): Unit = withContext(dispatcher) { openResult().getOrThrow() }

    public suspend fun migrate(vararg migrations: Migration): Unit = withContext(dispatcher) {
        migrateResult(*migrations).getOrThrow()
    }

    public suspend fun close(): Unit = withContext(dispatcher) { closeResult().getOrThrow() }

    public suspend inline fun <reified T : Any> insert(item: T): Unit = withContext(dispatcher) {
        insertResult(item).getOrThrow()
    }

    public suspend inline fun <reified T : Any> updateById(id: Long, item: T): Unit = withContext(dispatcher) {
        updateByIdResult(id, item).getOrThrow()
    }

    public suspend inline fun <reified T : Any> deleteById(id: Long): Unit = withContext(dispatcher) {
        deleteByIdResult<T>(id).getOrThrow()
    }

    public suspend inline fun <reified T : Any> getById(id: Long): T? = withContext(dispatcher) {
        getByIdResult<T>(id).getOrThrow()
    }

    public suspend inline fun <reified T : Any> list(
        limit: Int = 20,
        afterId: Long? = null,
        noinline idSelector: (T) -> Long,
    ): Page<T> = withContext(dispatcher) {
        listResult(limit, afterId, idSelector).getOrThrow()
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
