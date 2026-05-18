package com.example.myandroidapp.data.usecase

import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    companion object {
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }

    suspend operator fun invoke(articleId: Int): Result<Article> {
        return withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            repository.getArticle(articleId)
        } ?: Result.failure(Exception("Request timed out"))
    }
}
