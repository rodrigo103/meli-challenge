package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myandroidapp.ui.articles.detail.ArticleDetailPaneViewModel
import com.example.myandroidapp.ui.articles.detail.articleDetailContentSettings
import com.example.myandroidapp.ui.articles.list.ArticlesListActions
import com.example.myandroidapp.ui.articles.list.ArticlesListAttributes
import com.example.myandroidapp.ui.articles.list.ArticlesListScreen
import com.example.myandroidapp.ui.articles.list.ArticlesListViewModel

@Composable
fun DualPaneScreen(
    modifier: Modifier = Modifier,
    listViewModel: ArticlesListViewModel = hiltViewModel(),
    detailViewModel: ArticleDetailPaneViewModel = hiltViewModel(),
) {
    var selectedArticleId by remember { mutableStateOf<Int?>(null) }
    val searchQuery by listViewModel.searchQuery.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(LIST_WEIGHT).fillMaxSize()) {
                ArticlesListScreen(
                    attributes = ArticlesListAttributes(
                        searchQuery = searchQuery,
                        articles = listViewModel.articles,
                    ),
                    actions = ArticlesListActions(
                        onSearchTextChange = listViewModel::onSearchTextChange,
                        onClearSearch = listViewModel::clearSearch,
                        onArticleClick = { selectedArticleId = it },
                        sendAnalytics = listViewModel::sendAnalytics,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Box(modifier = Modifier.weight(DETAIL_WEIGHT).fillMaxSize()) {
                DetailPane(
                    articleId = selectedArticleId,
                    viewModel = detailViewModel,
                )
            }
        }
    }
}

@Composable
private fun DetailPane(
    articleId: Int?,
    viewModel: ArticleDetailPaneViewModel,
) {
    if (articleId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Select an article",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LaunchedEffect(articleId) {
            viewModel.loadArticle(articleId)
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = s.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            is UiState.Success -> articleDetailContentSettings(
                article = s.data.article)()
        }
    }
}

private const val LIST_WEIGHT = 0.4f
private const val DETAIL_WEIGHT = 0.6f
