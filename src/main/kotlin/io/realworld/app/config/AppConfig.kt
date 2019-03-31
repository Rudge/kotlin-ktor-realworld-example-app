package io.realworld.app.config

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.realworld.app.web.articles
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.profiles
import io.realworld.app.web.tags
import io.realworld.app.web.users

fun Application.mainModule() {
    install(ContentNegotiation) {
        jackson {
        }
    }
    routing {
        users()
        profiles()
        articles(ArticleController(), CommentController())
        tags()
    }
}
