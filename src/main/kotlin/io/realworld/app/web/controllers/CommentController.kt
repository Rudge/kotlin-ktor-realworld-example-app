package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import io.realworld.app.domain.CommentDTO

class CommentController {
    //class CommentController(private val commentService: CommentService) {
    suspend fun add(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]
        ctx.receive<CommentDTO>()
//                commentService.add(slug, ctx.attribute("email")!!, this.comment!!).also {
//                    ctx.json(CommentDTO(it))
//                }

    }

    fun findBySlug(ctx: ApplicationCall) {
        ctx.parameters["slug"]
//            commentService.findBySlug(this).also { comments ->
//                ctx.json(CommentsDTO(comments))
//            }

    }

    fun delete(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]
        val id = ctx.parameters["id"]
//        commentService.delete(id, slug)
    }

}
