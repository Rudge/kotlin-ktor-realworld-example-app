package io.realworld.app.web

internal data class ErrorResponse(val errors: Map<String, List<String?>>)

object ErrorExceptionMapping {
}
