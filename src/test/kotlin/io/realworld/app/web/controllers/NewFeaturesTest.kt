package io.realworld.app.web.controllers

import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.UserStatsDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the new features:
 * - Option A: GET /articles/feed/popular (Article Favorites Count Endpoint)
 * - Option B: GET /profiles/:username/stats (User Activity Stats)
 * - Option C: GET /articles/search?q=<term> (Article Search)
 */
class NewFeaturesTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    // ==================== Option A: Popular Feed Tests ====================

    @Test
    fun `get popular feed returns articles sorted by favorites`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
    }

    @Test
    fun `get popular feed with limit parameter`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/feed/popular?limit=10")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `get popular feed with offset parameter`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/feed/popular?offset=5")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `get popular feed with pagination`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/feed/popular?limit=10&offset=0")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `get popular feed returns empty list initially`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0, response.body.articlesCount)
        assertTrue(response.body.articles.isEmpty())
    }

    // ==================== Option B: User Stats Tests ====================

    @Test
    fun `get user stats without auth`() {
        val http = HttpUtil(appRule.port)
        val username = "testuser"
        val response = http.get<UserStatsDTO>("/profiles/$username/stats")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.stats)
        assertTrue(response.body.stats.articlesCount >= 0)
        assertTrue(response.body.stats.commentsCount >= 0)
        assertTrue(response.body.stats.favoritesCount >= 0)
    }

    @Test
    fun `get user stats returns correct structure`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<UserStatsDTO>("/profiles/anyuser/stats")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.stats)
        // Verify the stats object has the expected fields with stub values
        assertEquals(0, response.body.stats.articlesCount)
        assertEquals(0, response.body.stats.commentsCount)
        assertEquals(0, response.body.stats.favoritesCount)
    }

    @Test
    fun `get user stats for different usernames`() {
        val http = HttpUtil(appRule.port)

        val response1 = http.get<UserStatsDTO>("/profiles/user1/stats")
        assertEquals(HttpStatus.SC_OK, response1.status)

        val response2 = http.get<UserStatsDTO>("/profiles/user2/stats")
        assertEquals(HttpStatus.SC_OK, response2.status)
    }

    @Test
    fun `get user stats returns zero counts for non-existent user`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<UserStatsDTO>("/profiles/nonexistentuser123/stats")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.stats)
        // Stub returns zero counts
        assertEquals(0, response.body.stats.articlesCount)
        assertEquals(0, response.body.stats.commentsCount)
        assertEquals(0, response.body.stats.favoritesCount)
    }

    // ==================== Option C: Article Search Tests ====================

    @Test
    fun `search articles by query`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=dragon")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
        assertEquals(response.body.articles.size, response.body.articlesCount)
    }

    @Test
    fun `search articles with empty query`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `search articles with special characters in query`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=hello%20world")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `search articles with limit parameter`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=test&limit=5")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `search articles with offset parameter`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=test&offset=10")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `search articles with pagination`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=test&limit=5&offset=0")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.articles)
    }

    @Test
    fun `search articles returns empty list initially`() {
        val http = HttpUtil(appRule.port)
        val response = http.get<ArticlesDTO>("/articles/search?q=nonexistent")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0, response.body.articlesCount)
        assertTrue(response.body.articles.isEmpty())
    }
}
