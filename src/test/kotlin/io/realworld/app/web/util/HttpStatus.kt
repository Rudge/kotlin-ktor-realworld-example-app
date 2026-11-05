package io.realworld.app.web.util

/** HTTP status codes used by the tests (replaces org.apache.http.HttpStatus, formerly pulled in by Unirest). */
object HttpStatus {
    const val SC_OK = 200
    const val SC_FORBIDDEN = 403
    const val SC_UNPROCESSABLE_ENTITY = 422
}
