package io.github.androidpoet.kdb.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

public data class SqlStatement(
    val sql: String,
    val args: List<SqlArg> = emptyList()
)

public data class SqlArg(
    val value: SqlValue,
    val index: Int? = null
)

public sealed interface SqlValue {
    public data object Null : SqlValue
    public data class LongValue(val value: kotlin.Long) : SqlValue
    public data class DoubleValue(val value: kotlin.Double) : SqlValue
    public data class TextValue(val value: String) : SqlValue
    public data class BlobValue(val value: ByteArray) : SqlValue {
        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other !is BlobValue) return false
            return value.contentEquals(other.value)
        }
        override fun hashCode(): Int = value.contentHashCode()
    }
    public data class BooleanValue(val value: kotlin.Boolean) : SqlValue
    public data class InstantValue(val value: Instant) : SqlValue
    public data class LocalDateTimeValue(val value: LocalDateTime) : SqlValue
}

public interface KdbLogger {
    public fun d(message: String)
    public fun i(message: String)
    public fun w(message: String, throwable: Throwable? = null)
    public fun e(message: String, throwable: Throwable? = null)
}
