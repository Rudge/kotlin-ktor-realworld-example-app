package io.realworld.app.web.controllers

import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.UserDTO
import io.realworld.app.domain.repository.Articles
import io.realworld.app.domain.repository.Favorites
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ArticlePopularControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    /**
     * The app uses a *named* in-memory H2 database and never closes the Hikari pool, so rows
     * survive across test methods in the same JVM. Reset the article tables before each test so
     * the absolute-count assertions below stay deterministic regardless of execution order.
     */
    @Before
    fun resetArticleTables() {
        transaction {
            Favorites.deleteAll()
            Articles.deleteAll()
        }
    }

    private fun registerAndGetId(email: String, username: String): Long {
        appRule.http.registerUser(email, "password", username)
        appRule.http.loginAndSetTokenHeader(email, "password")
        val current = appRule.http.get<UserDTO>("/api/user")
        return current.body.user!!.id!!
    }

    private fun seedArticle(slug: String, authorId: Long, createdAt: Long, favoritedBy: List<Long> = listOf()): Long {
        val articleId = transaction {
            Articles.insertAndGetId { row ->
                row[Articles.slug] = slug
                row[Articles.title] = slug
                row[Articles.description] = "description"
                row[Articles.body] = "body"
                row[Articles.authorId] = authorId
                row[Articles.createdAt] = createdAt
                row[Articles.updatedAt] = createdAt
            }.value
        }
        transaction {
            favoritedBy.forEach { userId ->
                Favorites.insert { row ->
                    row[Favorites.articleId] = articleId
                    row[Favorites.userId] = userId
                }
            }
        }
        return articleId
    }

    @Test
    fun `orders articles by favorites count descending and reflects the current user's favorites`() {
        val authorId = registerAndGetId("author@popular.test", "author_popular")
        val fanId = registerAndGetId("fan@popular.test", "fan_popular")

        seedArticle("least-favorited", authorId, createdAt = 1000L, favoritedBy = listOf())
        seedArticle("most-favorited", authorId, createdAt = 2000L, favoritedBy = listOf(authorId, fanId))
        seedArticle("mid-favorited", authorId, createdAt = 3000L, favoritedBy = listOf(fanId))

        // headers still carry the fan's token from registerAndGetId's last login
        val response = appRule.http.get<ArticlesDTO>("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(3, response.body.articlesCount)
        assertEquals(
            listOf("most-favorited", "mid-favorited", "least-favorited"),
            response.body.articles.map { it.slug }
        )
        assertEquals(2L, response.body.articles[0].favoritesCount)
        assertTrue(response.body.articles[0].favorited)
        assertFalse(response.body.articles[2].favorited)
    }

    @Test
    fun `supports limit and offset pagination`() {
        val authorId = registerAndGetId("author2@popular.test", "author_popular2")
        (1..5).forEach { i -> seedArticle("article-$i", authorId, createdAt = i * 1000L) }

        val page = appRule.http.get<ArticlesDTO>(
            "/api/articles/feed/popular",
            mapOf("limit" to 2, "offset" to 1)
        )

        assertEquals(HttpStatus.SC_OK, page.status)
        assertEquals(5, page.body.articlesCount)
        assertEquals(2, page.body.articles.size)
        // sorted by createdAt desc (all tied on 0 favorites): article-5, article-4, article-3, article-2, article-1
        assertEquals(listOf("article-4", "article-3"), page.body.articles.map { it.slug })
    }

    @Test
    fun `is publicly accessible without authentication`() {
        val response = appRule.http.get<ArticlesDTO>("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0, response.body.articlesCount)
    }

    @Test
    fun `invalid or out-of-range pagination params fall back to safe defaults instead of erroring`() {
        val authorId = registerAndGetId("author3@popular.test", "author_popular3")
        seedArticle("only-article", authorId, createdAt = 1000L)

        val response = appRule.http.get<ArticlesDTO>(
            "/api/articles/feed/popular",
            mapOf("limit" to "not-a-number", "offset" to "-5")
        )

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(1, response.body.articlesCount)
        assertEquals(listOf("only-article"), response.body.articles.map { it.slug })
    }

    @Test
    fun `response never exposes the author's password hash`() {
        val authorId = registerAndGetId("author4@popular.test", "author_popular4")
        // NB: nothing in the seeded data may contain the literal "password", or the assertion
        // below would fail for the wrong reason.
        seedArticle("credential-leak-check", authorId, createdAt = 1000L)

        val raw = appRule.http.getRaw("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, raw.status)
        assertFalse(raw.body.contains("password", ignoreCase = true))
    }
}
