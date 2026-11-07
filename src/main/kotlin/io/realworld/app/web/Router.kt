package io.realworld.app.web

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.controllers.ProfileController
import io.realworld.app.web.controllers.TagController
import io.realworld.app.web.controllers.UserController

fun Routing.users(userController: UserController) {
    route("users") {
        post { userController.register(call) }
        post("login") { userController.login(call) }
    }
    route("user") {
        authenticate {
            get { userController.getCurrent(call) }
            put { userController.update(call) }
        }
    }
}

fun Routing.profiles(profileController: ProfileController) {
    route("profiles/{username}") {
        authenticate(optional = true) {
            get { profileController.get(call) }
        }
        authenticate {
            route("follow") {
                post { profileController.follow(call) }
                delete { profileController.unfollow(call) }
            }
        }
    }
}

fun Routing.articles(articleController: ArticleController, commentController: CommentController) {
    route("articles") {
        authenticate {
            get("feed") { articleController.feed(call) }
            route("{slug}") {
                route("comments") {
                    post { commentController.add(call) }
                    authenticate(optional = true) {
                        get { commentController.findBySlug(call) }
                    }
                    delete("{id}") { commentController.delete(call) }
                }
                route("favorite") {
                    post { articleController.favorite(call) }
                    delete { articleController.unfavorite(call) }
                }
                get { articleController.get(call) }
                put { articleController.update(call) }
                delete { articleController.delete(call) }
            }
            authenticate(optional = true) {
                get { articleController.findBy(call) }
            }
            post { articleController.create(call) }
        }
    }
}

fun Routing.tags(tagController: TagController) {
    route("tags") {
        authenticate(optional = true) {
            get { tagController.get(call) }
        }
    }
}
