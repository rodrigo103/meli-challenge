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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository,
    private val analytics: AnalyticsHelper,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _searchDisplayQuery = MutableStateFlow("")
    val searchDisplayQuery: StateFlow<String> = _searchDisplayQuery.asStateFlow()

    private val _searchTrigger = MutableStateFlow<String?>(null)
    private val searchTrigger: StateFlow<String?> = _searchTrigger.asStateFlow()

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
    val articles: Flow<PagingData<Article>> = searchTrigger.flatMapLatest { query ->
        repository.getArticlesPaged(searchQuery = query)
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchDisplayQuery.value = query
    }

    fun search(query: String) {
        _searchDisplayQuery.value = query
        _searchTrigger.value = query.ifBlank { null }
    }

    fun clearSearch() {
        _searchDisplayQuery.value = ""
        _searchTrigger.value = null
    }
}
