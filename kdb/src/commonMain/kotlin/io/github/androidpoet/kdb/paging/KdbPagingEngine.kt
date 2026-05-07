package io.github.androidpoet.kdb.paging

import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.core.map
import io.github.androidpoet.kdb.query.KdbQueryEngine
import io.github.androidpoet.kdb.query.Table

public class KdbPagingEngine(
    private val queryEngine: KdbQueryEngine,
) {
    public fun <R : Any> list(
        table: Table<R>,
        limit: Int = 20,
        afterId: Long? = null,
        idSelector: (R) -> Long,
    ): KdbResult<Page<R>> {
        val requested = if (limit < 1) 1 else limit
        return queryEngine.listByIdDesc(table = table, limit = requested + 1, afterId = afterId)
            .map { rawItems ->
                val hasMore = rawItems.size > requested
                val items = if (hasMore) rawItems.take(requested) else rawItems
                val nextCursor = if (hasMore) PageCursor.IdCursor(idSelector(items.last())) else null
                Page(items = items, nextCursor = nextCursor, hasMore = hasMore)
            }
    }
}
