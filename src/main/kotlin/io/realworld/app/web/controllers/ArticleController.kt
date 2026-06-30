package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.exceptions.UnauthorizedException
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    fun findBy(ctx: ApplicationCall): ArticlesDTO {
        val tag = ctx.parameters["tag"]
        val author = ctx.parameters["author"]
        val favorited = ctx.parameters["favorited"]
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 1)
    }

    fun feed(ctx: ApplicationCall): ArticlesDTO {
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 1)
    }

    /**
     * GET /articles/feed/popular — articles sorted by favorites count (desc), paginated.
     * Requires JWT; query params [limit] (default 20, must be > 0) and [offset] (default 0, >= 0).
     */
    suspend fun popularFeed(ctx: ApplicationCall) {
        val limit = parseQueryInt(ctx.parameters["limit"], 20, "limit") { it > 0 }
        val offset = parseQueryInt(ctx.parameters["offset"], 0, "offset") { it >= 0 }
        val email = ctx.authentication.principal<User>()?.email
            ?: throw UnauthorizedException("Authentication required.")
        val (articles, count) = articleService.findPopular(email, limit, offset)
        ctx.respond(ArticlesDTO(articles, count))
    }

    fun get(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    /** POST /articles — creates an article for the authenticated user. Body: ArticleDTO. */
    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email
            ?: throw UnauthorizedException("Authentication required.")
        val article = ctx.receive<ArticleDTO>().article
            ?: throw IllegalArgumentException("Article is invalid.")
        require(!article.title.isNullOrBlank()) { "Article title is required." }
        require(!article.body.isBlank()) { "Article body is required." }
        ctx.respond(ArticleDTO(articleService.create(email, article)))
    }

    suspend fun update(ctx: ApplicationCall): ArticleDTO {
        val slug = ctx.parameters["slug"]
        ctx.receive<ArticleDTO>()
        return ArticleDTO(null)
    }

    fun delete(ctx: ApplicationCall) {
        ctx.parameters["slug"]
    }

    /** POST /articles/:slug/favorite — idempotently favorites an article for the authenticated user. */
    suspend fun favorite(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email
            ?: throw UnauthorizedException("Authentication required.")
        val slug = ctx.parameters["slug"]
            ?: throw IllegalArgumentException("Slug is required.")
        ctx.respond(ArticleDTO(articleService.favorite(email, slug)))
    }

    fun unfavorite(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    /**
     * Parses an integer query parameter with a [default], or throws 422 via [IllegalArgumentException].
     *
     * @param raw query string value (null/blank → [default])
     * @param default used when the param is omitted
     * @param name param name for error messages (e.g. "limit", "offset")
     * @param validate extra rule (e.g. { it > 0 } for limit)
     */
    private fun parseQueryInt(
        raw: String?,
        default: Int,
        name: String,
        validate: (Int) -> Boolean
    ): Int {
        if (raw.isNullOrBlank()) return default
        val value = raw.toIntOrNull()
            ?: throw IllegalArgumentException("$name must be a valid integer.")
        require(validate(value)) { "$name is invalid." }
        return value
    }
}
