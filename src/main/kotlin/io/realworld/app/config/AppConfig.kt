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
import org.kodein.di.instance

const val SERVER_PORT = 8080

fun setup(isCio: Boolean = true): EmbeddedServer<*, *> {
    DbConfig.setup("jdbc:h2:mem:DATABASE_TO_UPPER=false;", "sa", "")
    return if (isCio) server(CIO) else server(Netty)
}

private fun <TEngine : io.ktor.server.engine.ApplicationEngine,
    TConfiguration : io.ktor.server.engine.ApplicationEngine.Configuration> server(
    engine: io.ktor.server.engine.ApplicationEngineFactory<TEngine, TConfiguration>
): EmbeddedServer<TEngine, TConfiguration> {
    return embeddedServer(
        engine,
        port = SERVER_PORT,
        watchPaths = listOf("mainModule"),
        module = Application::mainModule
    )
}

fun Application.mainModule() {
    val userController by ModulesConfig.di.instance<UserController>()
    val profileController by ModulesConfig.di.instance<ProfileController>()
    val articleController by ModulesConfig.di.instance<ArticleController>()
    val commentController by ModulesConfig.di.instance<CommentController>()
    val tagController by ModulesConfig.di.instance<TagController>()

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
        exception<Exception> { call, cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.toString())))
            call.respond(HttpStatusCode.InternalServerError, errorResponse)
        }
    }

    routing {
        users(userController)
        profiles(profileController)
        articles(articleController, commentController)
        tags(tagController)
    }
}
