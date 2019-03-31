package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall

class ProfileController {
    //class ProfileController(private val userService: UserService) {
    fun get(ctx: ApplicationCall) {
        ctx.parameters["username"]
//            userService.getProfileByUsername(ctx.attribute("email")!!, usernameFollowing).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }

    fun follow(ctx: ApplicationCall) {
        ctx.parameters["username"]
//            userService.follow(ctx.attribute("email")!!, usernameToFollow).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }

    fun unfollow(ctx: ApplicationCall) {
        ctx.parameters["username"]
//            userService.unfollow(ctx.attribute("email")!!, usernameToUnfollow).also { profile ->
//                ctx.json(ProfileDTO(profile))
    }
}
