package io.github.androidpoet.kdb.core

public sealed interface KdbError {
    public val message: String
    public val cause: Throwable?

    public data class SqlError(
        override val message: String,
        override val cause: Throwable? = null,
        val errorCode: Int? = null
    ) : KdbError

    public data class ConstraintError(
        override val message: String,
        override val cause: Throwable? = null
    ) : KdbError

    public data class MigrationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : KdbError

    public data class DriverError(
        override val message: String,
        override val cause: Throwable? = null
    ) : KdbError

    public data class SerializationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : KdbError

    public data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : KdbError
}

public class KdbException(public val error: KdbError) : Exception(error.message, error.cause)
