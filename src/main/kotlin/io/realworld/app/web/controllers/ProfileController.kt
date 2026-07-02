package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.response.respond
import io.realworld.app.domain.ProfileDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.UserService

class ProfileController(private val userService: UserService) {
    suspend fun get(ctx: ApplicationCall) {
        val username = ctx.parameters["username"]
            ?: throw IllegalArgumentException("Username is required.")
        val email = ctx.authentication.principal<User>()?.email ?: ""
        ctx.respond(ProfileDTO(userService.getProfileByUsername(email, username)))
    }

    suspend fun follow(ctx: ApplicationCall) {
        val username = ctx.parameters["username"]
            ?: throw IllegalArgumentException("Username is required.")
        val email = requireEmail(ctx)
        ctx.respond(ProfileDTO(userService.follow(email, username)))
    }

    suspend fun unfollow(ctx: ApplicationCall) {
        val username = ctx.parameters["username"]
            ?: throw IllegalArgumentException("Username is required.")
        val email = requireEmail(ctx)
        ctx.respond(ProfileDTO(userService.unfollow(email, username)))
    }

    private fun requireEmail(ctx: ApplicationCall): String =
        ctx.authentication.principal<User>()?.email
            ?: throw IllegalArgumentException("User not logged or with invalid email.")
}
