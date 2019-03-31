package io.realworld.app.web

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.realworld.app.web.controllers.ArticleController

fun Routing.users() {
    route("users") {
        post { call.respond("") }
        post("login") { call.respond("") }
    }
    route("user") {
        get { call.respond("") }
        put { call.respond("") }
    }
}

fun Routing.profiles() {
    route("profiles/{username}") {
        get { call.respond("") }
        route("follow") {
            post { call.respond("") }
            delete { call.respond("") }
        }
    }
}

fun Routing.articles(articleController: ArticleController) {
    route("articles") {
        get("feed") { call.respond(articleController.feed(this.context)) }
        route("{slug}") {
            route("comments") {
                post { call.respond("") }
                get { call.respond("") }
                delete("{id}") { call.respond("") }
            }
            route("favorite") {
                post { call.respond(articleController.favorite(this.context)) }
                delete { call.respond(articleController.unfavorite(this.context)) }
            }
            get { call.respond(articleController.get(this.context)) }
            put { call.respond(articleController.update(this.context)) }
            delete { call.respond(articleController.delete(this.context)) }
        }
        get { call.respond(articleController.findBy(this.context)) }
        post { call.respond(articleController.create(this.context)) }
    }
}

fun Routing.tags() {
    route("tags") {
        get { call.respond("") }
    }
}
