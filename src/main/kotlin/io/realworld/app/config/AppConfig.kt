package io.realworld.app.config

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.realworld.app.utils.JwtProvider
import io.realworld.app.web.ErrorResponse
import io.realworld.app.web.articles
import io.realworld.app.web.controllers.ArticleController
import io.realworld.app.web.controllers.CommentController
import io.realworld.app.web.controllers.ProfileController
import io.realworld.app.web.controllers.TagController
import io.realworld.app.web.controllers.UserController
import io.realworld.app.web.profiles
import io.realworld.app.web.tags
import io.realworld.app.web.users
import org.kodein.di.direct
import org.kodein.di.instance

const val SERVER_PORT = 8080

fun setup(isCio: Boolean = true): EmbeddedServer<*, *> {
    DbConfig.setup("jdbc:h2:mem:DATABASE_TO_UPPER=false;", "sa", "")
    return if (isCio) {
        embeddedServer(CIO, port = SERVER_PORT, module = Application::mainModule)
    } else {
        embeddedServer(Netty, port = SERVER_PORT, module = Application::mainModule)
    }
}

fun Application.mainModule() {
    val userController = ModulesConfig.kodein.direct.instance<UserController>()
    val profileController = ModulesConfig.kodein.direct.instance<ProfileController>()
    val articleController = ModulesConfig.kodein.direct.instance<ArticleController>()
    val commentController = ModulesConfig.kodein.direct.instance<CommentController>()
    val tagController = ModulesConfig.kodein.direct.instance<TagController>()

    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
        }
    }
    install(Authentication) {
        jwt {
            verifier(JwtProvider.verifier)
            authSchemes("Token")
            validate { credential ->
                if (credential.payload.audience.contains(JwtProvider.audience)) {
                    userController.getUserByEmail(credential.payload.claims["email"]?.asString())
                } else null
            }
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.toString())))
            call.respond(HttpStatusCode.InternalServerError, errorResponse)
        }
    }

    routing {
        route("api") {
            users(userController)
            profiles(profileController)
            articles(articleController, commentController)
            tags(tagController)
        }
    }
}
