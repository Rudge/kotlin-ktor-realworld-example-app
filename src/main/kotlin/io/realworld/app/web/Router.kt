package io.realworld.app.web

import io.ktor.auth.authenticate
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.controllers.ProfileController
import io.realworld.app.web.controllers.TagController
import io.realworld.app.web.controllers.UserController

fun Routing.users(userController: UserController) {
    route("users") {
        post { userController.register(this.context) }
        post("login") { userController.login(this.context) }
    }
    route("user") {
        authenticate {
            get { userController.getCurrent(this.context) }
            put { userController.update(this.context) }
        }
    }
}

fun Routing.profiles(profileController: ProfileController) {
    route("profiles/{username}") {
        authenticate(optional = true) {
            get { profileController.get(this.context) }
        }
        authenticate {
            route("follow") {
                post { profileController.follow(this.context) }
                delete { profileController.unfollow(this.context) }
            }
        }
    }
}

fun Routing.articles(articleController: ArticleController, commentController: CommentController) {
    route("articles") {
        authenticate {
            get("feed") { articleController.feed(this.context) }
            route("{slug}") {
                route("comments") {
                    post { commentController.add(this.context) }
                    authenticate(optional = true) {
                        get { commentController.findBySlug(this.context) }
                    }
                    delete("{id}") { commentController.delete(this.context) }
                }
                route("favorite") {
                    post { articleController.favorite(this.context) }
                    delete { articleController.unfavorite(this.context) }
                }
                get { articleController.get(this.context) }
                put { articleController.update(this.context) }
                delete { articleController.delete(this.context) }
            }
            authenticate(optional = true) {
                get { articleController.findBy(this.context) }
            }
            post { articleController.create(this.context) }
        }
    }
}

fun Routing.tags(tagController: TagController) {
    route("tags") {
        authenticate(optional = true) {
            get { tagController.get(this.context) }
        }
    }
}
