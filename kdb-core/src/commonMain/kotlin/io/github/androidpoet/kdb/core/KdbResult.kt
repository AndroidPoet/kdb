package io.github.androidpoet.kdb.core

/**
 * A discriminated result type for KDB operations.
 *
 * Every SDK call that can fail returns [KdbResult] instead of throwing,
 * giving callers full control over error handling via [map], [flatMap],
 * [recover], and friends.
 */
public sealed interface KdbResult<out T> {

    public data class Success<out T>(public val value: T) : KdbResult<T>

    public data class Failure(public val error: KdbError) : KdbResult<Nothing>

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw KdbException(error)
    }

    public fun errorOrNull(): KdbError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    public companion object {
        public fun <T> success(value: T): KdbResult<T> = Success(value)
        public fun failure(error: KdbError): KdbResult<Nothing> = Failure(error)

        /**
         * Executes [block] and wraps the outcome in a [KdbResult].
         */
        public inline fun <T> catching(block: () -> T): KdbResult<T> =
            try {
                Success(block())
            } catch (e: KdbException) {
                Failure(e.error)
            } catch (e: Exception) {
                Failure(KdbError.UnknownError(message = e.message ?: "Unknown error", cause = e))
            }
    }
}

// ── Extension functions ─────────────────────────────────────────────────

/**
 * Transforms the success value, leaving failures untouched.
 */
public inline fun <T, R> KdbResult<T>.map(
    transform: (T) -> R,
): KdbResult<R> = when (this) {
    is KdbResult.Success -> KdbResult.Success(transform(value))
    is KdbResult.Failure -> this
}

/**
 * Transforms the success value into another [KdbResult], flattening
 * the nesting.
 */
public inline fun <T, R> KdbResult<T>.flatMap(
    transform: (T) -> KdbResult<R>,
): KdbResult<R> = when (this) {
    is KdbResult.Success -> transform(value)
    is KdbResult.Failure -> this
}

/**
 * Invokes [action] when the result is a success, returning `this` for chaining.
 */
public inline fun <T> KdbResult<T>.onSuccess(
    action: (T) -> Unit,
): KdbResult<T> = apply {
    if (this is KdbResult.Success) action(value)
}

/**
 * Invokes [action] when the result is a failure, returning `this` for chaining.
 */
public inline fun <T> KdbResult<T>.onFailure(
    action: (KdbError) -> Unit,
): KdbResult<T> = apply {
    if (this is KdbResult.Failure) action(error)
}

/**
 * Attempts to recover from a failure by producing a new success value.
 */
public inline fun <T> KdbResult<T>.recover(
    transform: (KdbError) -> T,
): KdbResult<T> = when (this) {
    is KdbResult.Success -> this
    is KdbResult.Failure -> KdbResult.Success(transform(error))
}

/**
 * Returns the success value or the result of [defaultValue] on failure.
 */
public inline fun <T> KdbResult<T>.getOrElse(
    defaultValue: (KdbError) -> T,
): T = when (this) {
    is KdbResult.Success -> value
    is KdbResult.Failure -> defaultValue(error)
}
