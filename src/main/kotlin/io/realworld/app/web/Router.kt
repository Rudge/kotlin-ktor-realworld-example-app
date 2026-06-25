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
    route("api/users") {
        post { userController.register(this.context) }
        post("login") { userController.login(this.context) }
    }
    route("api/user") {
        authenticate {
            get { userController.getCurrent(this.context) }
            put { userController.update(this.context) }
        }
    }
}

fun Routing.profiles(profileController: ProfileController) {
    route("api/profiles/{username}") {
        authenticate(optional = true) {
            get { profileController.get(this.context) }
        }
        authenticate {
            post("follow") { profileController.follow(this.context) }
            delete("follow") { profileController.unfollow(this.context) }
        }
    }
}

fun Routing.articles(articleController: ArticleController, commentController: CommentController) {
    route("api/articles") {
        authenticate(optional = true) {
            get { articleController.findBy(this.context) }
        }
        authenticate {
            get("feed") { articleController.feed(this.context) }
            post { articleController.create(this.context) }
        }
        route("{slug}") {
            authenticate(optional = true) {
                get { articleController.get(this.context) }
                get("comments") { commentController.findBySlug(this.context) }
            }
            authenticate {
                put { articleController.update(this.context) }
                delete { articleController.delete(this.context) }
                post("favorite") { articleController.favorite(this.context) }
                delete("favorite") { articleController.unfavorite(this.context) }
                post("comments") { commentController.add(this.context) }
                delete("comments/{id}") { commentController.delete(this.context) }
            }
        }
    }
}

fun Routing.tags(tagController: TagController) {
    route("api/tags") {
        authenticate(optional = true) {
            get { tagController.get(this.context) }
        }
    }
}
