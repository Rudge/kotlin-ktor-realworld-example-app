package io.realworld.app.web.controllers

import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PopularArticleControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `get popular articles returns empty list when no articles exist`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertTrue(response.body.articles.isEmpty())
        assertEquals(0, response.body.articlesCount)
    }

    @Test
    fun `get popular articles sorted by favorites count descending`() {
        appRule.http.createUser()

        val responseA = appRule.http.post<ArticleDTO>(
            "/api/articles",
            ArticleDTO(Article(title = "Popular Article", description = "Most favorited", body = "body", tagList = listOf("popular")))
        )
        val responseB = appRule.http.post<ArticleDTO>(
            "/api/articles",
            ArticleDTO(Article(title = "Less Popular Article", description = "Less favorited", body = "body", tagList = listOf("less")))
        )

        // Favorite article A (1 favorite), leave B at 0 favorites
        appRule.http.post<ArticleDTO>("/api/articles/${responseA.body.article?.slug}/favorite")

        val response = appRule.http.get<ArticlesDTO>("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertEquals(2, response.body.articlesCount)
        assertEquals(responseA.body.article?.slug, response.body.articles.first().slug)
        assertTrue(response.body.articles.first().favoritesCount > 0)
        assertEquals(0L, response.body.articles.last().favoritesCount)
        assertNotNull(responseB.body.article?.slug)
    }

    @Test
    fun `get popular articles without authentication returns 200`() {
        appRule.http.createArticle()

        val http = HttpUtil(appRule.port)  // no auth token
        val response = http.get<ArticlesDTO>("/api/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
    }

    @Test
    fun `get popular articles with malformed token returns 401 not 500`() {
        // Ktor 1.x rejects a malformed JWT with 401 even when auth is optional —
        // providing a token signals intent to authenticate, so an invalid one is rejected.
        // This ensures bad credentials never cause a server error (5xx).
        val response = Unirest
            .get("http://localhost:${appRule.port}/api/articles/feed/popular")
            .header("Accept", "application/json")
            .header("Authorization", "Token invalid.jwt.token")
            .asString()

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `get popular articles respects limit parameter`() {
        appRule.http.createUser()
        repeat(3) { i ->
            appRule.http.post<ArticleDTO>(
                "/api/articles",
                ArticleDTO(Article(title = "Article $i", description = "desc", body = "body", tagList = listOf("tag$i")))
            )
        }

        val response = appRule.http.get<ArticlesDTO>("/api/articles/feed/popular?limit=2")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(2, response.body.articles.size)
        assertEquals(2, response.body.articlesCount)
    }
}
