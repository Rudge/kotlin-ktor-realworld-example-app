package io.realworld.app.web

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.root() {
    get("/") {
        call.respond(Pair("test", "Hello World"))
    }
}
