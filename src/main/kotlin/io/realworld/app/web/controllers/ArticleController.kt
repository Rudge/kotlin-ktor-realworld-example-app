package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO

class ArticleController {
//class ArticleController(private val articleService: ArticleService) {

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

    suspend fun create(ctx: ApplicationCall): ArticleDTO {
        ctx.receive<ArticleDTO>()
        //            articleService.create(ctx.attribute("email"), article).apply {
//                ctx.json(ArticleDTO(this))
//            }
        return ArticleDTO(null)
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

    fun favorite(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        //            articleService.favorite(ctx.attribute("email"), slug).apply {
//                ctx.json(ArticleDTO(this))
//            }
        return ArticleDTO(null)
    }

    fun unfavorite(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        //            articleService.unfavorite(ctx.attribute("email"), slug).apply {
//                ctx.json(ArticleDTO(this))
//            }
        return ArticleDTO(null)
    }
}
