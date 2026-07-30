package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.exceptions.UnauthorizedException
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    suspend fun popularFeed(ctx: ApplicationCall) {
        val limit = ctx.parameters["limit"].toPositiveIntOr(DEFAULT_LIMIT, "limit")
        val offset = ctx.parameters["offset"].toPositiveIntOr(0, "offset", allowZero = true)
        require(limit <= MAX_LIMIT) { "limit must not be greater than $MAX_LIMIT." }
        ctx.respond(articleService.findPopular(ctx.viewerId(), limit, offset))
    }

    private fun String?.toPositiveIntOr(default: Int, name: String, allowZero: Boolean = false): Int {
        if (this == null) return default
        val value = toIntOrNull()
        require(value != null && (value > 0 || (allowZero && value == 0))) {
            "$name must be a positive integer."
        }
        return value
    }

    fun findBy(ctx: ApplicationCall): ArticlesDTO {
        val tag = ctx.parameters["tag"]
        val author = ctx.parameters["author"]
        val favorited = ctx.parameters["favorited"]
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
//        articleService.findBy(tag, author, favorited, limit.toInt(), offset.toInt()).also { articles ->
//            ctx.json(ArticlesDTO(articles, articles.size))
//        }
        return ArticlesDTO(listOf(), 1)
    }

    fun feed(ctx: ApplicationCall): ArticlesDTO {
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
//        articleService.findFeed(ctx.attribute("email"), limit.toInt(), offset.toInt()).also { articles ->
//            ctx.json(ArticlesDTO(articles, articles.size))
//        }
        return ArticlesDTO(listOf(), 1)
    }

    fun get(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        //                articleService.findBySlug(slug).apply {
//                    ctx.json(ArticleDTO(this))
//                }
        return ArticleDTO(null)
    }

    suspend fun create(ctx: ApplicationCall) {
        val article = ctx.receive<ArticleDTO>().validToCreate()
        ctx.respond(ArticleDTO(articleService.create(article, ctx.viewerId())))
    }

    suspend fun update(ctx: ApplicationCall): ArticleDTO {
        val slug = ctx.parameters["slug"]
        ctx.receive<ArticleDTO>()
        //            articleService.update(slug, article).apply {
//                ctx.json(ArticleDTO(this))
//            }
        return ArticleDTO(null)
    }

    fun delete(ctx: ApplicationCall) {
        ctx.parameters["slug"]
        //            articleService.delete(slug)
    }

    suspend fun favorite(ctx: ApplicationCall) {
        ctx.respond(ArticleDTO(articleService.favorite(ctx.slug(), ctx.viewerId())))
    }

    suspend fun unfavorite(ctx: ApplicationCall) {
        ctx.respond(ArticleDTO(articleService.unfavorite(ctx.slug(), ctx.viewerId())))
    }

    private fun ApplicationCall.slug(): String =
        parameters["slug"].also { require(!it.isNullOrBlank()) { "Article slug is required." } }!!

    /** The JWT feature rejects anonymous callers first; this guards the id the principal carries. */
    private fun ApplicationCall.viewerId(): Long =
        authentication.principal<User>()?.id ?: throw UnauthorizedException("User not logged.")

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 100
    }
}
