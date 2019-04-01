package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.TagDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class TagControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `get all tags`() {
        val article = Article(
            title = "How to train your dragon",
            description = "Ever wonder how?",
            body = "Very carefully.",
            tagList = listOf("dragons", "training")
        )
        val email = "create_article@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "user_name_test")
        appRule.http.loginAndSetTokenHeader(email, password)

        appRule.http.post<ArticleDTO>("/api/articles", ArticleDTO(article))

        val response = appRule.http.get<TagDTO>("/api/tags")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertTrue(response.body.tags.isNotEmpty())
    }
}
