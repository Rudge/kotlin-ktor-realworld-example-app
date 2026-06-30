package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.ext.toSlug
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for GET /articles/feed/popular (Option A: popular articles by favorites count).
 * Uses [AppRule] to start a fresh server + DB per test. Runs with `./gradlew test` (not @Ignore).
 */
class PopularArticleFeedControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    /** Happy path: three articles with different favorite counts, assert sort order and counts. */
    @Test
    fun `returns articles sorted by favorites count descending`() {
        val http = authenticatedHttp("popular-sorted@example.com", "popular_sorted")

        val lowSlug = createArticle(http, "Few favorites", "few-favorites")
        val highSlug = createArticle(http, "Many favorites", "many-favorites")
        val midSlug = createArticle(http, "Some favorites", "some-favorites")

        favoriteAs(_http = http, email = "fan1@example.com", username = "fan1", slug = highSlug)
        favoriteAs(_http = http, email = "fan2@example.com", username = "fan2", slug = highSlug)
        favoriteAs(_http = http, email = "fan3@example.com", username = "fan3", slug = highSlug)
        favoriteAs(_http = http, email = "fan4@example.com", username = "fan4", slug = midSlug)
        favoriteAs(_http = http, email = "fan5@example.com", username = "fan5", slug = midSlug)
        favoriteAs(_http = http, email = "fan6@example.com", username = "fan6", slug = lowSlug)

        val response = http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(3, response.body.articlesCount)
        assertEquals(3, response.body.articles.size)
        assertEquals("many-favorites", response.body.articles[0].slug)
        assertEquals(3, response.body.articles[0].favoritesCount)
        assertEquals("some-favorites", response.body.articles[1].slug)
        assertEquals(2, response.body.articles[1].favoritesCount)
        assertEquals("few-favorites", response.body.articles[2].slug)
        assertEquals(1, response.body.articles[2].favoritesCount)
    }

    /** Pagination: limit=1 offset=1 returns the second-ranked article only; articlesCount is total. */
    @Test
    fun `supports limit and offset pagination`() {
        val http = authenticatedHttp("popular-page@example.com", "popular_page")

        val firstSlug = createArticle(http, "First article", "first-article")
        val secondSlug = createArticle(http, "Second article", "second-article")

        favoriteAs(_http = http, email = "pager1@example.com", username = "pager1", slug = firstSlug)
        favoriteAs(_http = http, email = "pager2@example.com", username = "pager2", slug = firstSlug)
        favoriteAs(_http = http, email = "pager3@example.com", username = "pager3", slug = secondSlug)

        val response = http.get<ArticlesDTO>(
            "/articles/feed/popular",
            mapOf("limit" to 1, "offset" to 1)
        )

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(2, response.body.articlesCount)
        assertEquals(1, response.body.articles.size)
        assertEquals("second-article", response.body.articles[0].slug)
    }

    /** Error case: no Authorization header → 401 (endpoint requires authentication). */
    @Test
    fun `rejects unauthenticated requests`() {
        val http = HttpUtil(appRule.port)
        val response = http.getRaw("/articles/feed/popular")

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    /** Error case: negative limit → 422 with message mentioning limit. */
    @Test
    fun `rejects invalid limit`() {
        val http = authenticatedHttp("popular-invalid@example.com", "popular_invalid")
        createArticle(http, "Validation article", "validation-article")

        val response = http.getRaw("/articles/feed/popular", mapOf("limit" to -1))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("limit"))
    }

    /** Error case: non-numeric offset → 422 with message mentioning offset. */
    @Test
    fun `rejects non-numeric offset`() {
        val http = authenticatedHttp("popular-offset@example.com", "popular_offset")
        createArticle(http, "Offset article", "offset-article")

        val response = http.getRaw(
            "/articles/feed/popular",
            mapOf("offset" to "abc")
        )

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("offset"))
    }

    /** Registers and logs in a user; returns HttpUtil with Authorization header set. */
    private fun authenticatedHttp(email: String, username: String): HttpUtil {
        val http = HttpUtil(appRule.port)
        http.registerUser(email, "password", username)
        http.loginAndSetTokenHeader(email, "password")
        return http
    }

    /** POST /articles with [title] and one [tag]; returns the generated slug for follow-up calls. */
    private fun createArticle(http: HttpUtil, title: String, tag: String): String {
        val expectedSlug = title.toSlug()
        val response = http.post<ArticleDTO>(
            "/articles",
            ArticleDTO(
                Article(
                    title = title,
                    description = "Description for $title",
                    body = "Body for $title",
                    tagList = listOf(tag)
                )
            )
        )
        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.article)
        assertEquals(expectedSlug, response.body.article?.slug)
        return expectedSlug
    }

    /** Registers a separate user and favorites [slug] — simulates multiple users favoriting one article. */
    private fun favoriteAs(_http: HttpUtil, email: String, username: String, slug: String) {
        val fan = HttpUtil(appRule.port)
        fan.registerUser(email, "password", username)
        fan.loginAndSetTokenHeader(email, "password")
        fan.post<ArticleDTO>("/articles/$slug/favorite")
    }
}
