package io.realworld.app.web

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
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

fun Route.users(userController: UserController) {
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

fun Route.profiles(profileController: ProfileController) {
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

fun Route.articles(articleController: ArticleController, commentController: CommentController) {
    route("articles") {
        route("feed") {
            authenticate {
                get { articleController.feed(call) }
            }
            authenticate(optional = true) {
                get("popular") { articleController.popular(call) }
            }
        }
        authenticate(optional = true) {
            get { articleController.findBy(call) }
        }
        authenticate {
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
            post { articleController.create(call) }
        }
    }
}

fun Route.tags(tagController: TagController) {
    route("tags") {
        authenticate(optional = true) {
            get { tagController.get(call) }
        }
    }
}
