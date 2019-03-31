package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import io.realworld.app.domain.UserDTO

class UserController {
    //class UserController(private val userService: UserService) {
    suspend fun login(ctx: ApplicationCall) {
        ctx.receive<UserDTO>()

//            userService.authenticate(user).apply {
//                ctx.json(UserDTO(this))
//            }
    }

    suspend fun register(ctx: ApplicationCall) {
        ctx.receive<UserDTO>()

//            userService.create(user).apply {
//                ctx.json(UserDTO(this))
//            }
    }

    fun getCurrent(ctx: ApplicationCall) {
//        userService.getByEmail(ctx.parameters["email"]).also { user ->
//            ctx.json(UserDTO(user))
//        }
    }

    suspend fun update(ctx: ApplicationCall) {
        val email = ctx.parameters["email"]
        ctx.receive<UserDTO>()
//            userService.update(email, user).apply {
//                ctx.json(UserDTO(this))
//            }
    }
}
