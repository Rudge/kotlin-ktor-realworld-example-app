package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.UserRepository

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) {
    fun findBy(tag: String?, author: String?, favorited: String?, currentUserId: Long?, limit: Int, offset: Int): List<Article> =
        articleRepository.findBy(tag, author, favorited, currentUserId, limit, offset)

    fun findPopular(currentUserId: Long?, limit: Int, offset: Int): List<Article> =
        articleRepository.findPopular(currentUserId, limit, offset)

    fun findFeed(currentUserId: Long?, limit: Int, offset: Int): List<Article> =
        articleRepository.findFeed(currentUserId, limit, offset)

    fun findBySlug(slug: String, currentUserId: Long? = null): Article? =
        articleRepository.findBySlug(slug, currentUserId)

    fun create(email: String, article: Article): Article {
        val user = userRepository.findByEmail(email) ?: error("User not found: $email")
        val slug = article.title!!.toSlug()
        return articleRepository.create(article.copy(slug = slug), user.id!!)
    }

    fun update(slug: String, article: Article, currentUserId: Long?): Article? =
        articleRepository.update(slug, article, currentUserId)

    fun delete(slug: String) = articleRepository.delete(slug)

    fun favorite(email: String, slug: String): Article? {
        val user = userRepository.findByEmail(email) ?: error("User not found: $email")
        return articleRepository.favorite(slug, user.id!!)
    }

    fun unfavorite(email: String, slug: String): Article? {
        val user = userRepository.findByEmail(email) ?: error("User not found: $email")
        return articleRepository.unfavorite(slug, user.id!!)
    }
}

private fun String.toSlug() = this.toLowerCase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
