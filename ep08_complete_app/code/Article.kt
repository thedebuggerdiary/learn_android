package com.debuggerdiary.ep08

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val url: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val source: String,
    @ColumnInfo(name = "published_at") val publishedAt: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ArticleDto(
    val title: String,
    val description: String?,
    @SerialName("urlToImage") val imageUrl: String?,
    val url: String,
    val source: SourceDto,
    val publishedAt: String
)

@Serializable
data class SourceDto(val name: String)

@Serializable
data class HeadlinesResponse(
    val articles: List<ArticleDto>
)

fun ArticleDto.toEntity() = Article(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    source = source.name,
    publishedAt = publishedAt
)
