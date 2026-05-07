package io.github.androidpoet.kdb.paging

public data class Page<T>(
    val items: List<T>,
    val nextCursor: PageCursor?,
    val hasMore: Boolean,
)

public sealed interface PageCursor {
    public data class IdCursor(val lastId: Long) : PageCursor
}
