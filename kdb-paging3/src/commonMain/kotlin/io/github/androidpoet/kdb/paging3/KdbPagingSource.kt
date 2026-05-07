package io.github.androidpoet.kdb.paging3

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.androidpoet.kdb.paging.KdbPagingEngine
import io.github.androidpoet.kdb.paging.PageCursor
import io.github.androidpoet.kdb.query.Table

/**
 * A Paging 3 [PagingSource] implementation for KDB keyset pagination.
 */
public class KdbPagingSource<R : Any>(
    private val pagingEngine: KdbPagingEngine,
    private val table: Table<R>,
    private val idSelector: (R) -> Long,
) : PagingSource<Long, R>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, R> {
        return try {
            val result = pagingEngine
                .list(table = table, limit = params.loadSize, afterId = params.key, idSelector = idSelector)
                .getOrThrow()

            val nextKey = when (val cursor = result.nextCursor) {
                is PageCursor.IdCursor -> cursor.lastId
                null -> null
            }

            LoadResult.Page(
                data = result.items,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, R>): Long? = null
}
