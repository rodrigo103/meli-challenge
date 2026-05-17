package com.example.myandroidapp.ui.articles.list

import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.preferences.AppPreferences
import com.example.myandroidapp.test.MainDispatcherRule
import androidx.paging.PagingData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArticlesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mockk<AnalyticsHelper>(relaxed = true)
    private val preferences = mockk<AppPreferences>(relaxed = true)

    @Test
    fun `onSearchQueryChanged updates display query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.onSearchQueryChanged("nasa")

        assertEquals("nasa", viewModel.searchDisplayQuery.value)
    }

    @Test
    fun `clearSearch resets display query`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.search("nasa")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchDisplayQuery.value)
    }

    @Test
    fun `onArticleSelected updates selectedArticleId`() = runTest {
        val repository = mockk<ArticlesRepository>()
        every { repository.getArticlesPaged(any()) } returns flowOf(PagingData.empty())

        val viewModel = ArticlesListViewModel(repository, analytics, preferences)

        viewModel.onArticleSelected(42)

        assertEquals(42, viewModel.selectedArticleId.value)
    }
}
