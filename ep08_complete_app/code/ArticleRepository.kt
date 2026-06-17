package com.debuggerdiary.ep08

import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

class ArticleRepository @Inject constructor(
    private val api: NewsApi,
    private val dao: ArticleDao
) {
    val articles: Flow<List<Article>> = dao.getAllArticles()

    suspend fun refresh() {
        try {
            val response = api.getTopHeadlines()
            val entities = response.articles
                .filter { it.url.isNotBlank() && it.title != "[Removed]" }
                .map { it.toEntity() }
            dao.clearAll()
            dao.insertAll(entities)
        } catch (e: IOException) {
            // Network unavailable — cached data stays visible
        } catch (e: retrofit2.HttpException) {
            // API error (403, 429, etc.) — cached data stays visible
        }
    }
}
