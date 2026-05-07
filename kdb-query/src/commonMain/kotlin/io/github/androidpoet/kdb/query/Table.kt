package io.github.androidpoet.kdb.query

import io.github.androidpoet.kdb.core.SqlValue
import io.github.androidpoet.kdb.driver.KdbCursor

public interface Table<R : Any> {
    public val tableName: String
    public val columns: List<Column<*>>
    
    public fun fromRow(cursor: KdbCursor): R
    public fun toValues(row: R): Map<String, SqlValue>
}

public data class Column<T>(
    val name: String,
    val type: ColumnType
)

public enum class ColumnType {
    LONG, DOUBLE, TEXT, BLOB, BOOLEAN, INSTANT, LOCAL_DATE_TIME
}
