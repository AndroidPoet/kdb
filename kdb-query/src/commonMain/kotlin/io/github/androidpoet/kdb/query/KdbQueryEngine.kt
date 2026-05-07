package io.github.androidpoet.kdb.query

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.SqlValue
import io.github.androidpoet.kdb.core.flatMap
import io.github.androidpoet.kdb.core.map
import io.github.androidpoet.kdb.driver.KdbDriver

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

    public fun <R : Any> selectAll(table: Table<R>): KdbResult<List<R>> {
        val sql = "SELECT * FROM ${table.tableName}"
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

    private fun io.github.androidpoet.kdb.driver.KdbPreparedStatement.bind(index: Int, value: SqlValue) {
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
