package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.UserRepository

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) {
    fun create(email: String, article: Article): Article {
        val authorId = requireUserId(email)
        return articleRepository.create(article, authorId)
    }

    fun favorite(email: String, slug: String): Article {
        val userId = requireUserId(email)
        return articleRepository.favorite(userId, slug)
    }

    fun unfavorite(email: String, slug: String): Article {
        val userId = requireUserId(email)
        return articleRepository.unfavorite(userId, slug)
    }

    fun findPopular(email: String, limit: Int, offset: Int): ArticlesDTO {
        val userId = requireUserId(email)
        val articles = articleRepository.findPopular(userId, limit, offset)
        return ArticlesDTO(articles, articleRepository.countAll())
    }

    private fun requireUserId(email: String): Long =
        userRepository.findByEmail(email)?.id
            ?: throw NotFoundException("User not found for email.")
}
