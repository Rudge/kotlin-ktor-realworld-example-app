package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    // Out-of-scope stub: the generic article-list endpoint (GET /articles) is NOT part of
    // the Popular Articles Feed feature and is intentionally not implemented -- it returns an
    // empty result regardless of input. The pagination/filter params are parsed here as a
    // template for a future implementation, which must delegate to the service to query data.
    // NOTE: read limit/offset from ctx.request.queryParameters (NOT ctx.parameters, which
    // exposes only PATH params -- the original scaffold stub used ctx.parameters, so its
    // ?limit=/?offset= would never have been read). The fully-implemented, correctly
    // paginated endpoint is popularFeed() below.
    @Suppress("UNUSED_VARIABLE") // params parsed as a template until this endpoint is implemented
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

    // Out-of-scope stub: the personalized "following" feed (GET /articles/feed) is not part
    // of this feature. Same caveat as findBy -- pagination is read from queryParameters (with
    // safe defaults) as a template, but no data is queried yet.
    @Suppress("UNUSED_VARIABLE") // params parsed as a template until this endpoint is implemented
    fun feed(ctx: ApplicationCall): ArticlesDTO {
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtLeast(0) ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return ArticlesDTO(listOf(), 0)
    }

    suspend fun popularFeed(ctx: ApplicationCall) {
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtLeast(0) ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val email = requireEmail(ctx)
        ctx.respond(articleService.findPopular(email, limit, offset))
    }

    fun get(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    suspend fun create(ctx: ApplicationCall) {
        val article = ctx.receive<ArticleDTO>().validCreate()
        val email = requireEmail(ctx)
        ctx.respond(ArticleDTO(articleService.create(email, article)))
    }

    suspend fun update(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        ctx.receive<ArticleDTO>()
        return ArticleDTO(null)
    }

    fun delete(ctx: ApplicationCall) {
        ctx.parameters["slug"]
    }

    suspend fun favorite(ctx: ApplicationCall) {
        val slug = requireSlug(ctx)
        val email = requireEmail(ctx)
        ctx.respond(ArticleDTO(articleService.favorite(email, slug)))
    }

    suspend fun unfavorite(ctx: ApplicationCall) {
        val slug = requireSlug(ctx)
        val email = requireEmail(ctx)
        ctx.respond(ArticleDTO(articleService.unfavorite(email, slug)))
    }

    private fun requireEmail(ctx: ApplicationCall): String =
        ctx.authentication.principal<User>()?.email
            ?: throw IllegalArgumentException("User not logged or with invalid email.")

    private fun requireSlug(ctx: ApplicationCall): String =
        ctx.parameters["slug"] ?: throw IllegalArgumentException("Article slug is required.")
}
