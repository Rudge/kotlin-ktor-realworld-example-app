package io.realworld.app.domain.service

import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.repository.ArticleRepository

class ArticleService(private val articleRepository: ArticleRepository) {
    fun findPopular(limit: Int?, offset: Int?, currentUserId: Long?): ArticlesDTO {
        val safeLimit = (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val safeOffset = (offset ?: DEFAULT_OFFSET).coerceAtLeast(0)
        val articles = articleRepository.findPopular(safeLimit, safeOffset, currentUserId)
        return ArticlesDTO(articles, articleRepository.countAll())
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val DEFAULT_OFFSET = 0
        private const val MAX_LIMIT = 100
    }
}
