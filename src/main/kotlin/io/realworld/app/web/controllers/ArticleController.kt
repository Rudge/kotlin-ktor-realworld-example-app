package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    suspend fun findBy(ctx: ApplicationCall) {
        val tag = ctx.request.queryParameters["tag"]
        val author = ctx.request.queryParameters["author"]
        val favorited = ctx.request.queryParameters["favorited"]
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val currentUser = ctx.authentication.principal<User>()
        val articles = articleService.findBy(tag, author, favorited, currentUser?.id, limit, offset)
        ctx.respond(ArticlesDTO(articles, articles.size))
    }

    suspend fun feed(ctx: ApplicationCall) {
        val limit = ctx.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val offset = ctx.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val currentUser = ctx.authentication.principal<User>()
        val articles = articleService.findFeed(currentUser?.id, limit, offset)
        ctx.respond(ArticlesDTO(articles, articles.size))
    }

    suspend fun get(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val currentUser = ctx.authentication.principal<User>()
        val article = articleService.findBySlug(slug, currentUser?.id)
        if (article != null) ctx.respond(ArticleDTO(article))
        else ctx.respond(HttpStatusCode.NotFound)
    }

    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email!!
        val articleDTO = ctx.receive<ArticleDTO>()
        val article = articleService.create(email, articleDTO.article!!)
        ctx.respond(ArticleDTO(article))
    }

    suspend fun update(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val currentUser = ctx.authentication.principal<User>()
        val articleDTO = ctx.receive<ArticleDTO>()
        val article = articleService.update(slug, articleDTO.article!!, currentUser?.id)
        if (article != null) ctx.respond(ArticleDTO(article))
        else ctx.respond(HttpStatusCode.NotFound)
    }

    suspend fun delete(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        articleService.delete(slug)
        ctx.respond(HttpStatusCode.OK)
    }

    suspend fun favorite(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val email = ctx.authentication.principal<User>()?.email!!
        val article = articleService.favorite(email, slug)
        if (article != null) ctx.respond(ArticleDTO(article))
        else ctx.respond(HttpStatusCode.NotFound)
    }

    suspend fun unfavorite(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val email = ctx.authentication.principal<User>()?.email!!
        val article = articleService.unfavorite(email, slug)
        if (article != null) ctx.respond(ArticleDTO(article))
        else ctx.respond(HttpStatusCode.NotFound)
    }
}
