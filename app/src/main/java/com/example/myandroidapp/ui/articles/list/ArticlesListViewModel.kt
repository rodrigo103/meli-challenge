package com.example.myandroidapp.ui.articles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.Article
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ArticlesListState(
    val searchQuery: String = "",
)

data class ArticlesListActions(
    val onArticleClick: (Int) -> Unit,
    val onSearch: (String) -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
)

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedArticleId = MutableStateFlow<Int?>(null)
    val selectedArticleId: StateFlow<Int?> = _selectedArticleId.asStateFlow()

    fun onArticleSelected(id: Int) {
        _selectedArticleId.value = id
        analytics.logEvent("article_selected", mapOf("id" to id.toString()))
    }

    init {
        analytics.logScreenView("ArticlesList")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: Flow<PagingData<Article>> = _searchQuery.flatMapLatest { query ->
        repository.getArticlesPaged(searchQuery = query.ifBlank { null })
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}
