package io.realworld.app.web

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.exceptions.UnauthorizedException

internal data class ErrorResponse(val errors: Map<String, List<String?>>)

private fun errorBody(message: String?) = ErrorResponse(mapOf("body" to listOf(message)))

fun StatusPages.Configuration.errorExceptionMapping() {
    exception<NotFoundException> { cause ->
        call.respond(HttpStatusCode.NotFound, errorBody(cause.message))
    }
    exception<UnauthorizedException> { cause ->
        call.respond(HttpStatusCode.Unauthorized, errorBody(cause.message))
    }
    exception<IllegalArgumentException> { cause ->
        call.respond(HttpStatusCode.UnprocessableEntity, errorBody(cause.message))
    }
    exception<Throwable> { _ ->
        call.respond(HttpStatusCode.InternalServerError, errorBody("An unexpected error occurred."))
    }
}
