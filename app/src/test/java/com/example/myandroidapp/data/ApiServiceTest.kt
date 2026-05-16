package com.example.myandroidapp.data

import com.example.myandroidapp.TestJson
import com.example.myandroidapp.rules.MockWebServerRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ApiServiceTest {

    @get:Rule
    val serverRule = MockWebServerRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun createApi(): ApiService {
        return Retrofit.Builder()
            .baseUrl(serverRule.baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    @Test
    fun `getArticles returns parsed list on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.ARTICLE_RESPONSE)

        val response = createApi().getArticles()

        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals(2, body.count)
        assertEquals(2, body.results.size)
        assertEquals("Article 1", body.results[0].title)
        assertEquals("NASA", body.results[0].newsSite)
        assertEquals("Article 2", body.results[1].title)
        assertEquals("SpaceX", body.results[1].newsSite)
    }

    @Test
    fun `getArticles returns failure on 500`() = runTest {
        serverRule.enqueueJson(500, """{"error":"Internal error"}""")

        val response = createApi().getArticles()

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `getArticles returns failure on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        val response = createApi().getArticles()

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun `getArticle returns parsed article on 200`() = runTest {
        serverRule.enqueueJson(200, TestJson.SINGLE_ARTICLE)

        val response = createApi().getArticle(1)

        assertTrue(response.isSuccessful)
        val article = response.body()!!
        assertEquals("Article Detail", article.title)
        assertEquals("Full summary", article.summary)
        assertEquals("NASA", article.newsSite)
    }

    @Test
    fun `getArticle returns failure on 404`() = runTest {
        serverRule.enqueueJson(404, """{"error":"Not found"}""")

        val response = createApi().getArticle(999)

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun `getArticles handles null fields gracefully`() = runTest {
        serverRule.enqueueJson(200, """
            {
              "count": 1,
              "next": null,
              "previous": null,
              "results": [
                {
                  "id": 1,
                  "title": "Article 1",
                  "summary": "",
                  "image_url": null,
                  "news_site": null,
                  "published_at": null,
                  "url": "",
                  "authors": []
                }
              ]
            }
        """.trimIndent())

        val response = createApi().getArticles()

        assertTrue(response.isSuccessful)
        val article = response.body()!!.results[0]
        assertTrue(article.imageUrl == null)
        assertTrue(article.newsSite == null)
        assertTrue(article.publishedAt == null)
    }
}
