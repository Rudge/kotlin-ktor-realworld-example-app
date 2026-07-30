package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.repository.ArticleRepository

class ArticleService(private val articleRepository: ArticleRepository) {

    fun findPopular(viewerId: Long, limit: Int, offset: Int) =
        ArticlesDTO(articleRepository.findPopular(viewerId, limit, offset), articleRepository.count().toInt())

    fun create(article: Article, authorId: Long): Article =
        articleRepository.findBySlug(articleRepository.create(article, authorId), authorId)

    fun favorite(slug: String, userId: Long): Article {
        articleRepository.favorite(slug, userId)
        return articleRepository.findBySlug(slug, userId)
    }

    fun unfavorite(slug: String, userId: Long): Article {
        articleRepository.unfavorite(slug, userId)
        return articleRepository.findBySlug(slug, userId)
    }
}
