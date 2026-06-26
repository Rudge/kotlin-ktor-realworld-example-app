package io.realworld.app.web.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.ObjectMapper
import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO

class HttpUtil(port: Int) {
    private val json = "application/json"
    val headers = mutableMapOf("Accept" to json, "Content-Type" to json)

    init {
        Unirest.setObjectMapper(object : ObjectMapper {
            override fun <T> readValue(value: String, valueType: Class<T>): T {
                return jacksonObjectMapper().readValue(value, valueType)
            }

            override fun writeValue(value: Any): String {
                return jacksonObjectMapper().writeValueAsString(value)
            }
        })
    }

    @JvmField
    val origin: String = "http://localhost:$port"

    inline fun <reified T> post(path: String) =
        Unirest.post(origin + path).headers(headers).asObject(T::class.java)

    inline fun <reified T> post(path: String, body: Any) =
        Unirest.post(origin + path).headers(headers).body(body).asObject(T::class.java)

    inline fun <reified T> get(path: String, params: Map<String, Any>? = null) =
        Unirest.get(origin + path).headers(headers).queryString(params).asObject(T::class.java)

    inline fun <reified T> put(path: String, body: Any) =
        Unirest.put(origin + path).headers(headers).body(body).asObject(T::class.java)

    inline fun <reified T> deleteWithResponseBody(path: String) =
        Unirest.delete(origin + path).headers(headers).asObject(T::class.java)

    fun delete(path: String) =
        Unirest.delete(origin + path).headers(headers).asString()

    fun loginAndSetTokenHeader(email: String, password: String) {
        val userDTO = UserDTO(User(email = email, password = password))
        val response = post<UserDTO>("/api/users/login", userDTO)
        headers["Authorization"] = "Token ${response.body.user?.token}"
    }

    fun registerUser(email: String, password: String, username: String): UserDTO {
        val userDTO = UserDTO(User(email = email, password = password, username = username))
        val response = post<UserDTO>("/api/users", userDTO)
        return response.body
    }

    fun createUser(userEmail: String = "user@valid_user_mail.com", username: String = "user_name_test"): UserDTO {
        val password = "password"
        val user = registerUser(userEmail, password, username)
        loginAndSetTokenHeader(userEmail, password)
        return user
    }

    fun createArticle(article: Article): HttpResponse<ArticleDTO> {
        createUser()
        return post<ArticleDTO>("/api/articles", ArticleDTO(article))
    }

    fun createArticle(): HttpResponse<ArticleDTO> {
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
