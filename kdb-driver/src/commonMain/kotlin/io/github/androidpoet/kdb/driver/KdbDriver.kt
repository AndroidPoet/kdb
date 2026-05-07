package io.github.androidpoet.kdb.driver

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.SqlValue

public interface KdbDriver {
    public fun open(): KdbResult<Unit>
    public fun close(): KdbResult<Unit>
    public fun <T> transaction(block: () -> T): KdbResult<T>
    public fun prepare(sql: String): KdbResult<KdbPreparedStatement>
    public fun execute(sql: String, args: List<SqlValue> = emptyList()): KdbResult<Unit>
}

public interface KdbPreparedStatement {
    public fun bindNull(index: Int)
    public fun bindLong(index: Int, value: Long)
    public fun bindDouble(index: Int, value: Double)
    public fun bindText(index: Int, value: String)
    public fun bindBlob(index: Int, value: ByteArray)
    
    public fun execute(): KdbResult<Unit>
    public fun executeQuery(): KdbResult<KdbCursor>
    public fun close()
}

public interface KdbCursor {
    public fun next(): Boolean
    public fun isNull(index: Int): Boolean
    public fun getLong(index: Int): Long
    public fun getDouble(index: Int): Double
    public fun getText(index: Int): String
    public fun getBlob(index: Int): ByteArray
    public fun getColumnIndex(name: String): Int
    public fun close()
}
