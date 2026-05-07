package io.github.androidpoet.kdb.paging

public data class Page<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
)

public data class KeysetCursor(
    val lastId: Long,
    val lastValue: Any? = null
)
