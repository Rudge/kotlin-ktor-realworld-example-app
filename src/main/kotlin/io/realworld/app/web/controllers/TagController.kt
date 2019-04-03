package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.response.respond
import io.realworld.app.domain.service.TagService

class TagController(private val tagService: TagService) {
    suspend fun get(ctx: ApplicationCall) = tagService.findAll().also { tagDto ->
        ctx.respond(tagDto)
    }
}
