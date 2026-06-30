package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.repository.ArticleRepository

/** Service layer for article write/read operations used by [ArticleController]. */
class ArticleService(private val articleRepository: ArticleRepository) {

    /** Creates an article authored by the user identified by [email]. */
    fun create(email: String, article: Article): Article = articleRepository.create(email, article)

    /** Adds a favorite for [email] on the article with [slug]. */
    fun favorite(email: String, slug: String): Article = articleRepository.favorite(email, slug)

    /**
     * Popular feed: articles ordered by favorites count with pagination.
     *
     * @param email viewer email (from JWT); passed through for favorited flags on each article
     * @param limit page size; must be > 0
     * @param offset number of articles to skip; must be >= 0
     */
    fun findPopular(email: String?, limit: Int, offset: Int): Pair<List<Article>, Int> {
        require(limit > 0) { "limit must be a positive integer." }
        require(offset >= 0) { "offset must be a non-negative integer." }
        return articleRepository.findPopular(email, limit, offset)
    }
}
