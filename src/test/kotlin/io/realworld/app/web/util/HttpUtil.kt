package io.realworld.app.web.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Minimal response wrapper exposing the `status` and `body` accessors the tests rely on. */
data class Response<T>(val status: Int, val body: T)

class HttpUtil(port: Int) {
    private val json = "application/json"
    val headers = mutableMapOf("Accept" to json, "Content-Type" to json)
    val mapper: ObjectMapper = jacksonObjectMapper()
    val client: HttpClient = HttpClient.newHttpClient()

    @JvmField
    val origin: String = "http://localhost:$port"

    fun send(method: String, path: String, body: Any? = null): HttpResponse<String> {
        val publisher = if (body == null) {
            HttpRequest.BodyPublishers.noBody()
        } else {
            HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))
        }
        val builder = HttpRequest.newBuilder(URI.create(origin + path)).method(method, publisher)
        headers.forEach { (name, value) -> builder.header(name, value) }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    inline fun <reified T> parse(response: HttpResponse<String>): Response<T> =
        Response(response.statusCode(), mapper.readValue(response.body(), T::class.java))

    inline fun <reified T> post(path: String) = parse<T>(send("POST", path))

    inline fun <reified T> post(path: String, body: Any) = parse<T>(send("POST", path, body))

    inline fun <reified T> get(path: String, params: Map<String, Any>? = null): Response<T> {
        val query = params?.entries?.joinToString("&", prefix = "?") { (key, value) ->
            "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(value.toString(), Charsets.UTF_8)}"
        } ?: ""
        return parse<T>(send("GET", path + query))
    }

    inline fun <reified T> put(path: String, body: Any) = parse<T>(send("PUT", path, body))

    inline fun <reified T> deleteWithResponseBody(path: String) = parse<T>(send("DELETE", path))

    fun delete(path: String): Response<String> = send("DELETE", path).let { Response(it.statusCode(), it.body()) }

    fun loginAndSetTokenHeader(email: String, password: String) {
        val userDTO = UserDTO(User(email = email, password = password))
        val response = post<UserDTO>("/users/login", userDTO)
        headers["Authorization"] = "Token ${response.body.user?.token}"
    }

    fun registerUser(email: String, password: String, username: String): UserDTO {
        val userDTO = UserDTO(User(email = email, password = password, username = username))
        val response = post<UserDTO>("/users", userDTO)
        return response.body
    }

    fun createUser(userEmail: String = "user@valid_user_mail.com", username: String = "user_name_test"): UserDTO {
        val password = "password"
        val user = registerUser(userEmail, password, username)
        loginAndSetTokenHeader(userEmail, password)
        return user
    }

    fun createArticle(article: Article): Response<ArticleDTO> {
        createUser()
        return post<ArticleDTO>("/api/articles", ArticleDTO(article))
    }

    fun createArticle(): Response<ArticleDTO> {
        return createArticle(
            Article(
                title = "How to train your dragon",
                description = "Ever wonder how?",
                body = "Very carefully.",
                tagList = listOf("dragons", "training")
            )
        )
    }
}
