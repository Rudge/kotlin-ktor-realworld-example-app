package io.realworld.app.config

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.exceptions.UnauthorizedException
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
import org.kodein.di.generic.instance

const val SERVER_PORT = 8080

/**
 * Boots the embedded Ktor server and connects to an in-memory H2 database.
 *
 * @param isCio when true uses the CIO engine (lighter, good for tests); otherwise Netty
 * @param port HTTP port to bind; defaults to [SERVER_PORT] for `./gradlew run`, tests pass a free port
 *
 * Each call uses a unique H2 database name (`test_<nanoTime>`) so integration tests get a clean
 * slate. Previously all tests shared one in-memory DB (`jdbc:h2:mem:DATABASE_TO_UPPER=false`), so
 * data from one test leaked into the next (e.g. articlesCount was 6 instead of 2). A unique name
 * gives each test run its own isolated database.
 */
@KtorExperimentalAPI
@EngineAPI
fun setup(isCio: Boolean = true, port: Int = SERVER_PORT): BaseApplicationEngine {
    val dbName = "test_${System.nanoTime()}"
    DbConfig.setup("jdbc:h2:mem:$dbName;DATABASE_TO_UPPER=false;", "sa", "")
    return server(if (isCio) CIO else Netty, port)
}

/**
 * Creates the embedded server instance. Extracted from [setup] only so tests can pass a custom
 * [port]; the body is otherwise unchanged from the original (engine, watchPaths, mainModule).
 */
@KtorExperimentalAPI
@EngineAPI
fun server(
    engine: ApplicationEngineFactory<BaseApplicationEngine,
        out ApplicationEngine.Configuration>,
    port: Int = SERVER_PORT
): BaseApplicationEngine {
    return embeddedServer(
        engine,
        port = port,
        watchPaths = listOf("mainModule"),
        module = Application::mainModule
    )
}

@KtorExperimentalAPI
fun Application.mainModule() {
    val userController by ModulesConfig.kodein.instance<UserController>()
    val profileController by ModulesConfig.kodein.instance<ProfileController>()
    val articleController by ModulesConfig.kodein.instance<ArticleController>()
    val commentController by ModulesConfig.kodein.instance<CommentController>()
    val tagController by ModulesConfig.kodein.instance<TagController>()

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
    // StatusPages existed before but only mapped every Exception → 500. The article endpoints
    // throw domain/validation exceptions that need proper HTTP status codes (401, 404, 422).
    // Existing user controllers did not rely on these handlers — they returned DTOs directly.
    // New article controllers throw instead, so these handlers are required for correct responses.
    install(StatusPages) {
        exception<UnauthorizedException> { cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.message)))
            context.respond(HttpStatusCode.Unauthorized, errorResponse)
        }
        exception<NotFoundException> { cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.message)))
            context.respond(HttpStatusCode.NotFound, errorResponse)
        }
        exception<IllegalArgumentException> { cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.message)))
            context.respond(HttpStatusCode.UnprocessableEntity, errorResponse)
        }
        exception<Exception> { cause ->
            val errorResponse = ErrorResponse(mapOf("error" to listOf("detail", cause.toString())))
            context.respond(
                HttpStatusCode.InternalServerError, errorResponse
            )
        }
    }

    install(Routing) {
        users(userController)
        profiles(profileController)
        articles(articleController, commentController)
        tags(tagController)
    }
}
