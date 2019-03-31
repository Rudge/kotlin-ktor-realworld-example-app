package io.realworld.app.config

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.realworld.app.web.root

fun Application.mainModule() {
    install(ContentNegotiation) {
        jackson {
        }
    }
    routing {
        root()
    }
}
