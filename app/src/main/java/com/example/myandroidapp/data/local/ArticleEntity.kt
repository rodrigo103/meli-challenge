package com.example.myandroidapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myandroidapp.data.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "news_site") val newsSite: String?,
    @ColumnInfo(name = "published_at") val publishedAt: String?,
    val url: String,
)

fun ArticleEntity.toArticle() = Article(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    newsSite = newsSite,
    publishedAt = publishedAt,
    url = url,
)

fun Article.toEntity() = ArticleEntity(
    id = id,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    newsSite = newsSite,
    publishedAt = publishedAt,
    url = url,
)
