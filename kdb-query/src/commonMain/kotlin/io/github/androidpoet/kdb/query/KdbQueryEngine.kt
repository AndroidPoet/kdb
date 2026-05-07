package io.github.androidpoet.kdb.query

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.SqlValue
import io.github.androidpoet.kdb.core.flatMap
import io.github.androidpoet.kdb.core.map
import io.github.androidpoet.kdb.driver.KdbDriver
import io.github.androidpoet.kdb.driver.KdbPreparedStatement

public class KdbQueryEngine(private val driver: KdbDriver) {

    public fun <R : Any> insert(table: Table<R>, row: R): KdbResult<Unit> {
        val values = table.toValues(row)
        val columnNames = values.keys.joinToString(", ")
        val placeholders = values.keys.joinToString(", ") { "?" }
        val sql = "INSERT INTO ${table.tableName} ($columnNames) VALUES ($placeholders)"

        return driver.prepare(sql).flatMap { statement ->
            values.values.forEachIndexed { index, sqlValue ->
                statement.bind(index + 1, sqlValue)
            }
            statement.execute().also { statement.close() }
        }
    }

    public fun <R : Any> updateById(table: Table<R>, row: R, id: Long): KdbResult<Unit> {
        val allValues = table.toValues(row)
        val values = allValues.filterKeys { it != table.idColumn }
        val setClause = values.keys.joinToString(", ") { "$it = ?" }
        val sql = "UPDATE ${table.tableName} SET $setClause WHERE ${table.idColumn} = ?"

        return driver.prepare(sql).flatMap { statement ->
            var bindIndex = 1
            values.values.forEach { value ->
                statement.bind(bindIndex++, value)
            }
            statement.bindLong(bindIndex, id)
            statement.execute().also { statement.close() }
        }
    }

    public fun <R : Any> deleteById(table: Table<R>, id: Long): KdbResult<Unit> {
        val sql = "DELETE FROM ${table.tableName} WHERE ${table.idColumn} = ?"
        return driver.prepare(sql).flatMap { statement ->
            statement.bindLong(1, id)
            statement.execute().also { statement.close() }
        }
    }

    public fun <R : Any> getById(table: Table<R>, id: Long): KdbResult<R?> {
        val sql = "SELECT * FROM ${table.tableName} WHERE ${table.idColumn} = ? LIMIT 1"
        return driver.prepare(sql).flatMap { statement ->
            statement.bindLong(1, id)
            statement.executeQuery().map { cursor ->
                val item = if (cursor.next()) table.fromRow(cursor) else null
                cursor.close()
                item
            }.also { statement.close() }
        }
    }

    public fun <R : Any> selectAll(table: Table<R>): KdbResult<List<R>> {
        val sql = "SELECT * FROM ${table.tableName} ORDER BY ${table.idColumn} DESC"
        return driver.prepare(sql).flatMap { statement ->
            statement.executeQuery().map { cursor ->
                val results = mutableListOf<R>()
                while (cursor.next()) {
                    results.add(table.fromRow(cursor))
                }
                cursor.close()
                results
            }.also { statement.close() }
        }
    }

    public fun <R : Any> listByIdDesc(table: Table<R>, limit: Int, afterId: Long? = null): KdbResult<List<R>> {
        val sql = if (afterId == null) {
            "SELECT * FROM ${table.tableName} ORDER BY ${table.idColumn} DESC LIMIT ?"
        } else {
            "SELECT * FROM ${table.tableName} WHERE ${table.idColumn} < ? ORDER BY ${table.idColumn} DESC LIMIT ?"
        }

        return driver.prepare(sql).flatMap { statement ->
            if (afterId == null) {
                statement.bindLong(1, limit.toLong())
            } else {
                statement.bindLong(1, afterId)
                statement.bindLong(2, limit.toLong())
            }
            statement.executeQuery().map { cursor ->
                val items = mutableListOf<R>()
                while (cursor.next()) {
                    items.add(table.fromRow(cursor))
                }
                cursor.close()
                items
            }.also { statement.close() }
        }
    }

    private fun KdbPreparedStatement.bind(index: Int, value: SqlValue) {
        when (value) {
            is SqlValue.Null -> bindNull(index)
            is SqlValue.LongValue -> bindLong(index, value.value)
            is SqlValue.DoubleValue -> bindDouble(index, value.value)
            is SqlValue.TextValue -> bindText(index, value.value)
            is SqlValue.BlobValue -> bindBlob(index, value.value)
            is SqlValue.BooleanValue -> bindLong(index, if (value.value) 1 else 0)
            is SqlValue.InstantValue -> bindLong(index, value.value.toEpochMilliseconds())
            is SqlValue.LocalDateTimeValue -> bindText(index, value.value.toString())
        }
    }
}
