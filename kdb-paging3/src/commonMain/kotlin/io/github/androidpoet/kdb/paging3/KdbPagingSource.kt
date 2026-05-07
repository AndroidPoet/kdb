package io.github.androidpoet.kdb.paging3

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.androidpoet.kdb.core.KdbResult
import io.github.androidpoet.kdb.paging.KdbPagingEngine
import io.github.androidpoet.kdb.query.Table

/**
 * A Paging 3 [PagingSource] implementation for KDB.
 * Uses keyset pagination based on a Long ID.
 */
public class KdbPagingSource<R : Any>(
    private val pagingEngine: KdbPagingEngine,
    private val table: Table<R>,
    private val idColumn: String = "id"
) : PagingSource<Long, R>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, R> {
        return try {
            val lastId = params.key
            val result = pagingEngine.selectPage(
                table = table,
                idColumn = idColumn,
                lastId = lastId,
                limit = params.loadSize
            ).getOrThrow()

            LoadResult.Page(
                data = result.items,
                prevKey = null, // Keyset pagination typically only goes forward easily
                nextKey = if (result.hasMore) {
                    // We need a way to extract the ID from the last item.
                    // This is why we assume the table can provide values.
                    val lastItem = result.items.last()
                    val values = table.toValues(lastItem)
                    val idValue = values[idColumn]
                    if (idValue is io.github.androidpoet.kdb.core.SqlValue.LongValue) {
                        idValue.value
                    } else null
                } else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, R>): Long? {
        // For keyset pagination, we usually refresh from the beginning (null)
        // or from the closest anchor point.
        return null
    }
}
