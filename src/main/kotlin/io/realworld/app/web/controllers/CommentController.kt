package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.CommentDTO
import io.realworld.app.domain.CommentsDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.CommentService

class CommentController(private val commentService: CommentService) {
    suspend fun add(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val email = ctx.authentication.principal<User>()?.email!!
        val commentDTO = ctx.receive<CommentDTO>()
        val comment = commentService.add(slug, email, commentDTO.comment!!)
        ctx.respond(CommentDTO(comment))
    }

    suspend fun findBySlug(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val comments = commentService.findBySlug(slug)
        ctx.respond(CommentsDTO(comments))
    }

    suspend fun delete(ctx: ApplicationCall) {
        val slug = ctx.parameters["slug"]!!
        val id = ctx.parameters["id"]!!.toLong()
        commentService.delete(id, slug)
        ctx.respond(HttpStatusCode.OK)
    }
}
