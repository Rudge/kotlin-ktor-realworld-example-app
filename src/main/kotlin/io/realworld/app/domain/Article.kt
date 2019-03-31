package io.realworld.app.domain

import java.util.*

data class ArticleDTO(val article: Article?)

data class ArticlesDTO(val articles: List<Article>, val articlesCount: Int)

data class Article(val slug: String? = null,
                   val title: String?,
                   val description: String?,
                   val body: String,
                   val tagList: List<String> = listOf(),
                   val createdAt: Date? = null,
                   val updatedAt: Date? = null,
                   val favorited: Boolean = false,
                   val favoritesCount: Long = 0,
                   val author: User? = null)