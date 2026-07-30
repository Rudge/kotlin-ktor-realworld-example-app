package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

/**
 * Covers the write path the popular feed ranks on: `POST /api/articles` and
 * `POST`/`DELETE /api/articles/{slug}/favorite`.
 *
 * Every test creates its own article, so the tests do not depend on each other's counts
 * or on the order JUnit happens to run them in.
 */
class ArticleFavoritesTest {

    companion object {
        private const val EMAIL = "favorites_author@valid_email.com"
        private const val PASSWORD = "password"

        @ClassRule
        @JvmField
        val appRule = AppRule()

        private val http: HttpUtil get() = appRule.http

        @BeforeClass
        @JvmStatic
        fun register() {
            http.registerAndLogin(EMAIL, "favorites_author", PASSWORD)
        }
    }

    private fun newArticle(title: String, tags: List<String> = listOf()): Article {
        val response = appRule.http.createArticle(
            Article(title = title, description = "about $title", body = "body of $title", tagList = tags)
        )
        assertEquals(HttpStatus.SC_OK, response.status)
        return response.body.article!!
    }

    @Before
    fun authenticate() {
        appRule.http.loginAndSetTokenHeader(EMAIL, PASSWORD)
    }

    @Test
    fun `create an article and derive its slug from the title`() {
        val article = newArticle("Create derives the slug", tags = listOf("kotlin", "ktor"))

        assertEquals("create-derives-the-slug", article.slug)
        assertEquals("favorites_author", article.author?.username)
        assertEquals(listOf("kotlin", "ktor"), article.tagList)
        assertEquals(0L, article.favoritesCount)
        assertFalse(article.favorited)
    }

    @Test
    fun `suffix the slug when a title collides`() {
        val first = newArticle("Colliding title")
        val second = newArticle("Colliding title")

        assertEquals("colliding-title", first.slug)
        assertEquals("colliding-title-2", second.slug)
        assertNotEquals(first.slug, second.slug)
    }

    @Test
    fun `favorite increments the count and flags the article`() {
        val slug = newArticle("Favorite increments").slug!!

        val response = appRule.http.favoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(1L, response.body.article?.favoritesCount)
        assertTrue(response.body.article!!.favorited)
    }

    @Test
    fun `favorite is idempotent`() {
        val slug = newArticle("Favorite is idempotent").slug!!

        appRule.http.favoriteArticle(slug)
        val second = appRule.http.favoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, second.status)
        assertEquals(1L, second.body.article?.favoritesCount)
    }

    @Test
    fun `unfavorite decrements the count and clears the flag`() {
        val slug = newArticle("Unfavorite decrements").slug!!
        appRule.http.favoriteArticle(slug)

        val response = appRule.http.unfavoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0L, response.body.article?.favoritesCount)
        assertFalse(response.body.article!!.favorited)
    }

    @Test
    fun `unfavorite an article that was never favorited is a no-op`() {
        val slug = newArticle("Unfavorite without favorite").slug!!

        val response = appRule.http.unfavoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0L, response.body.article?.favoritesCount)
    }

    @Test
    fun `return not found when favoriting an unknown slug`() {
        val favorite = appRule.http.postRaw("/api/articles/does-not-exist/favorite")
        val unfavorite = appRule.http.deleteRaw("/api/articles/does-not-exist/favorite")

        assertEquals(HttpStatus.SC_NOT_FOUND, favorite.status)
        assertTrue(favorite.body.contains("Article not found by slug"))
        assertEquals(HttpStatus.SC_NOT_FOUND, unfavorite.status)
    }

    @Test
    fun `return unprocessable entity when the title is blank`() {
        val response = appRule.http.postRaw(
            "/api/articles",
            ArticleDTO(Article(title = " ", description = "d", body = "b"))
        )

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("title and body are required"))
    }

    @Test
    fun `return unauthorized when creating without a token`() {
        appRule.http.clearTokenHeader()

        val response = appRule.http.postRaw(
            "/api/articles",
            ArticleDTO(Article(title = "No token", description = "d", body = "b"))
        )

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }
}
