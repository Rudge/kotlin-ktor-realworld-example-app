package io.realworld.app.web

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.exceptions.UnauthorizedException

internal data class ErrorResponse(val errors: Map<String, List<String?>>)

/**
 * RealWorld errors are always `{"errors": {"body": [...]}}`; only the status code varies.
 */
object ErrorExceptionMapping {

    fun setup(config: StatusPages.Configuration) {
        config.exception<IllegalArgumentException> { cause ->
            call.respondError(HttpStatusCode.UnprocessableEntity, cause.message)
        }
        config.exception<NotFoundException> { cause ->
            call.respondError(HttpStatusCode.NotFound, cause.message)
        }
        config.exception<UnauthorizedException> { cause ->
            call.respondError(HttpStatusCode.Unauthorized, cause.message)
        }
        config.exception<Exception> { cause ->
            call.respondError(HttpStatusCode.InternalServerError, cause.message)
        }
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String?) =
    respond(status, ErrorResponse(mapOf("body" to listOf(message ?: status.description))))
