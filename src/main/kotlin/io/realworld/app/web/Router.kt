package io.realworld.app.web

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route

fun Routing.root() {
    get("/") { call.respond(Pair("test", "Hello World")) }
    route("users") {
        post { call.respond("") }
        post("login") { call.respond("") }
    }
    route("user") {
        get { call.respond("") }
        put { call.respond("") }
    }
    route("profiles/:username") {
        get { call.respond("") }
        route("follow") {
            post { call.respond("") }
            delete { call.respond("") }
        }
    }
    route("articles") {
        get("feed") { call.respond("") }
        route(":slug") {
            route("comments") {
                post { call.respond("") }
                get { call.respond("") }
                delete(":id") { call.respond("") }
            }
            route("favorite") {
                post { call.respond("") }
                delete { call.respond("") }
            }
            get { call.respond("") }
            put { call.respond("") }
            delete { call.respond("") }
        }
        get { call.respond("") }
        post { call.respond("") }
    }
    route("tags") {
        get { call.respond("") }
    }
}
