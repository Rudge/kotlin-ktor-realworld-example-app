package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

/**
 * Integration tests for `GET /api/articles/feed/popular`.
 *
 * The fixture is built entirely over HTTP — register, create articles, favorite them — so the
 * tests exercise the same path a client takes, including the JWT the endpoint requires.
 */
class ArticlePopularFeedTest {

    companion object {
        private const val PATH = "/api/articles/feed/popular"
        private const val AUTHOR_EMAIL = "feed_author@valid_email.com"
        private const val READER_EMAIL = "feed_reader@valid_email.com"
        private const val OTHER_READER_EMAIL = "feed_other_reader@valid_email.com"
        private const val PASSWORD = "password"

        @ClassRule
        @JvmField
        val appRule = AppRule()

        private lateinit var slugs: Map<String, String>

        private val http: HttpUtil get() = appRule.http

        @BeforeClass
        @JvmStatic
        fun seed() {
            http.registerAndLogin(AUTHOR_EMAIL, "feed_author", PASSWORD)

            // Created oldest to newest; "delta" and "gamma" both end up with zero favorites,
            // which is what pins the newest-first tie-break below.
            slugs = listOf(
                "Popular alpha" to listOf("dragons", "training"),
                "Popular beta" to listOf<String>(),
                "Popular gamma" to listOf(),
                "Popular delta" to listOf()
            ).associate { (title, tags) ->
                val response = http.createArticle(
                    Article(title = title, description = "about $title", body = "body of $title", tagList = tags)
                )
                assertEquals(HttpStatus.SC_OK, response.status)
                title to response.body.article!!.slug!!
            }

            http.registerAndLogin(READER_EMAIL, "feed_reader", PASSWORD)
            http.favoriteArticle(slugs.getValue("Popular alpha"))
            http.favoriteArticle(slugs.getValue("Popular beta"))

            http.registerAndLogin(OTHER_READER_EMAIL, "feed_other_reader", PASSWORD)
            http.favoriteArticle(slugs.getValue("Popular alpha"))
        }

        private fun slugOf(title: String) = slugs.getValue(title)
    }

    @Before
    fun authenticateAsAuthor() {
        appRule.http.loginAndSetTokenHeader(AUTHOR_EMAIL, PASSWORD)
    }

    @Test
    fun `rank by favorites count descending, newest first on a tie`() {
        val response = appRule.http.get<ArticlesDTO>(PATH)

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(
            listOf("Popular alpha", "Popular beta", "Popular delta", "Popular gamma").map(::slugOf),
            response.body.articles.map { it.slug }
        )
        assertEquals(listOf(2L, 1L, 0L, 0L), response.body.articles.map { it.favoritesCount })
        assertEquals(4, response.body.articlesCount)
    }

    @Test
    fun `expose the author as a profile without credentials`() {
        val author = appRule.http.get<ArticlesDTO>(PATH).body.articles.first().author

        assertEquals("feed_author", author?.username)
        assertFalse(author!!.following)
        assertFalse("payload must not carry credentials", appRule.http.getRaw(PATH).body.contains("password"))
    }

    @Test
    fun `round-trip the tag list`() {
        val article = appRule.http.get<ArticlesDTO>(PATH).body.articles.first()

        assertEquals(listOf("dragons", "training"), article.tagList)
    }

    @Test
    fun `report favorited per viewer`() {
        appRule.http.loginAndSetTokenHeader(READER_EMAIL, PASSWORD)
        val asReader = appRule.http.get<ArticlesDTO>(PATH).body.articles

        assertEquals(listOf(true, true, false, false), asReader.map { it.favorited })

        appRule.http.loginAndSetTokenHeader(AUTHOR_EMAIL, PASSWORD)
        val asAuthor = appRule.http.get<ArticlesDTO>(PATH).body.articles

        assertTrue("the author favorited nothing", asAuthor.none { it.favorited })
    }

    @Test
    fun `paginate with limit and offset`() {
        val firstPage = appRule.http.get<ArticlesDTO>(PATH, mapOf("limit" to 2))
        val secondPage = appRule.http.get<ArticlesDTO>(PATH, mapOf("limit" to 2, "offset" to 2))

        assertEquals(HttpStatus.SC_OK, firstPage.status)
        assertEquals(listOf("Popular alpha", "Popular beta").map(::slugOf), firstPage.body.articles.map { it.slug })
        assertEquals(listOf("Popular delta", "Popular gamma").map(::slugOf), secondPage.body.articles.map { it.slug })
        // articlesCount is the total available, not the size of the page.
        assertEquals(4, firstPage.body.articlesCount)
        assertEquals(4, secondPage.body.articlesCount)
    }

    @Test
    fun `return an empty page when the offset is past the end`() {
        val response = appRule.http.get<ArticlesDTO>(PATH, mapOf("offset" to 99))

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articles.isEmpty())
        assertEquals(4, response.body.articlesCount)
    }

    @Test
    fun `return unauthorized without a token`() {
        appRule.http.clearTokenHeader()

        assertEquals(HttpStatus.SC_UNAUTHORIZED, appRule.http.getRaw(PATH).status)
    }

    @Test
    fun `return unprocessable entity for a non numeric limit`() {
        val response = appRule.http.getRaw(PATH, mapOf("limit" to "abc"))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("limit must be a positive integer"))
    }

    @Test
    fun `return unprocessable entity for a negative offset`() {
        val response = appRule.http.getRaw(PATH, mapOf("offset" to -1))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("offset must be a positive integer"))
    }

    @Test
    fun `return unprocessable entity for a limit above the maximum`() {
        val response = appRule.http.getRaw(PATH, mapOf("limit" to 101))

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
        assertTrue(response.body.contains("limit must not be greater than 100"))
    }
}
