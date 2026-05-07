package io.github.androidpoet.kdb.driver

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.androidpoet.kdb.core.KdbError
import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.SqlValue

public class CommonKdbDriver(private val fileName: String) : KdbDriver {
    private val driver = BundledSQLiteDriver()
    private var connection: SQLiteConnection? = null

    override fun open(): KdbResult<Unit> = try {
        connection = driver.open(fileName)
        KdbResult.success(Unit)
    } catch (e: Exception) {
        KdbResult.failure(KdbError.DriverError("Failed to open database: $fileName", e))
    }

    override fun close(): KdbResult<Unit> = try {
        connection?.close()
        connection = null
        KdbResult.success(Unit)
    } catch (e: Exception) {
        KdbResult.failure(KdbError.DriverError("Failed to close database", e))
    }

    override fun <T> transaction(block: () -> T): KdbResult<T> {
        val conn = connection ?: return KdbResult.failure(KdbError.DriverError("Database not open"))
        return try {
            conn.execHelper("BEGIN TRANSACTION")
            val result = block()
            conn.execHelper("COMMIT TRANSACTION")
            KdbResult.success(result)
        } catch (e: Exception) {
            try { conn.execHelper("ROLLBACK TRANSACTION") } catch (re: Exception) {}
            KdbResult.failure(KdbError.SqlError("Transaction failed", e))
        }
    }

    override fun prepare(sql: String): KdbResult<KdbPreparedStatement> {
        val conn = connection ?: return KdbResult.failure(KdbError.DriverError("Database not open"))
        return try {
            KdbResult.success(CommonPreparedStatement(conn.prepare(sql)))
        } catch (e: Exception) {
            KdbResult.failure(KdbError.SqlError("Failed to prepare statement: $sql", e))
        }
    }

    override fun execute(sql: String, args: List<SqlValue>): KdbResult<Unit> {
        val conn = connection ?: return KdbResult.failure(KdbError.DriverError("Database not open"))
        return try {
            if (args.isEmpty()) {
                conn.execHelper(sql)
            } else {
                conn.prepare(sql).use { statement ->
                    args.forEachIndexed { index, sqlValue ->
                        statement.bind(index + 1, sqlValue)
                    }
                    statement.step()
                }
            }
            KdbResult.success(Unit)
        } catch (e: Exception) {
            KdbResult.failure(KdbError.SqlError("Failed to execute SQL: $sql", e))
        }
    }

    private fun SQLiteConnection.execHelper(sql: String) {
        prepare(sql).use { it.step() }
    }

    private fun SQLiteStatement.bind(index: Int, value: SqlValue) {
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

private class CommonPreparedStatement(private val statement: SQLiteStatement) : KdbPreparedStatement {
    override fun bindNull(index: Int) = statement.bindNull(index)
    override fun bindLong(index: Int, value: Long) = statement.bindLong(index, value)
    override fun bindDouble(index: Int, value: Double) = statement.bindDouble(index, value)
    override fun bindText(index: Int, value: String) = statement.bindText(index, value)
    override fun bindBlob(index: Int, value: ByteArray) = statement.bindBlob(index, value)

    override fun execute(): KdbResult<Unit> = try {
        statement.step()
        KdbResult.success(Unit)
    } catch (e: Exception) {
        KdbResult.failure(KdbError.SqlError("Failed to execute statement", e))
    }

    override fun executeQuery(): KdbResult<KdbCursor> = try {
        // In androidx.sqlite, executeQuery doesn't return a cursor directly.
        // The statement itself IS the cursor (you call step() and then getXXX()).
        KdbResult.success(CommonKdbCursor(statement))
    } catch (e: Exception) {
        KdbResult.failure(KdbError.SqlError("Failed to execute query", e))
    }

    override fun close() = statement.close()
}

private class CommonKdbCursor(private val statement: SQLiteStatement) : KdbCursor {
    override fun next(): Boolean = statement.step()
    override fun isNull(index: Int): Boolean = statement.isNull(index)
    override fun getLong(index: Int): Long = statement.getLong(index)
    override fun getDouble(index: Int): Double = statement.getDouble(index)
    override fun getText(index: Int): String = statement.getText(index)
    override fun getBlob(index: Int): ByteArray = statement.getBlob(index)
    override fun getColumnIndex(name: String): Int = statement.getColumnName(0).let { 
        // Note: getColumnName requires index. To find index by name, we'd need to iterate or get all names.
        // This is a bit inefficient here but works for the interface.
        val count = statement.getColumnCount()
        for (i in 0 until count) {
            if (statement.getColumnName(i) == name) return i
        }
        -1
    }
    override fun close() {
        // Statement shouldn't be closed here because it's managed by CommonPreparedStatement
        // But the cursor interface has close(). We'll just reset it if possible or ignore if managed.
        statement.reset()
    }
}
