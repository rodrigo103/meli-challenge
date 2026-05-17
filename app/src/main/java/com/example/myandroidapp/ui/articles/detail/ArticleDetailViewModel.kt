package com.example.myandroidapp.ui.articles.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleDetailState(
    val article: Article,
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val articleId: Int = checkNotNull(savedStateHandle["articleId"]) { "articleId required" }

    private val _uiState = MutableStateFlow<UiState<ArticleDetailState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ArticleDetailState>> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    fun loadArticle() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            repository.getArticle(articleId)
                .onSuccess { article ->
                    _uiState.value = UiState.Success(ArticleDetailState(article = article))
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
