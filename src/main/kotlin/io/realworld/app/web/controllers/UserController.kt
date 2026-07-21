package io.realworld.app.web.controllers

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.domain.service.UserService

class UserController(private val userService: UserService) {
    suspend fun login(ctx: ApplicationCall) {
        ctx.receive<UserDTO>().apply {
            userService.authenticate(this.validLogin()).apply {
                ctx.respond(UserDTO(this))
            }
        }
    }

    suspend fun register(ctx: ApplicationCall) {
        ctx.receive<UserDTO>().apply {
            userService.create(this.validRegister()).apply {
                ctx.respond(UserDTO(this))
            }
        }
    }

    fun getUserByEmail(email: String?): User {
        return email.let {
            require(!it.isNullOrBlank()) { "User not logged or with invalid email." }
            userService.getByEmail(it)
        }
    }

    suspend fun getCurrent(ctx: ApplicationCall) {
        ctx.respond(UserDTO(ctx.principal<User>()))
    }

    suspend fun update(ctx: ApplicationCall) {
        val email = ctx.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged." }
        ctx.receive<UserDTO>().also { userDto ->
            userService.update(email, userDto.validToUpdate()).apply {
                ctx.respond(UserDTO(this))
            }
        }
    }
}
