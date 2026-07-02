package io.realworld.app.web.controllers

import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PopularArticlesFeedControllerTest {

    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `popular feed requires authentication`() {
        val response = Unirest.get("http://localhost:${appRule.port}/articles/feed/popular")
            .header("Accept", "application/json")
            .asString()
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `popular feed returns articles ordered by favorites count descending`() {
        val http = appRule.http

        http.registerAndLogin("order_author1@test.com", "order_author1")
        val art1 = http.post<ArticleDTO>("/articles", ArticleDTO(
            Article(title = "Order Test Article One", description = "desc one", body = "body one")
        )).body.article!!

        http.registerAndLogin("order_author2@test.com", "order_author2")
        val art2 = http.post<ArticleDTO>("/articles", ArticleDTO(
            Article(title = "Order Test Article Two", description = "desc two", body = "body two")
        )).body.article!!

        // voter1 + voter2 both favorite art1 → art1: 2, art2: 0
        http.registerAndLogin("order_voter1@test.com", "order_voter1")
        http.favoriteArticle(art1.slug!!)
        http.registerAndLogin("order_voter2@test.com", "order_voter2")
        http.favoriteArticle(art1.slug!!)

        val response = http.get<ArticlesDTO>("/articles/feed/popular")
        assertEquals(HttpStatus.SC_OK, response.status)

        val articles = response.body.articles
        assertTrue("Feed should not be empty", articles.isNotEmpty())

        val idx1 = articles.indexOfFirst { it.slug == art1.slug }
        val idx2 = articles.indexOfFirst { it.slug == art2.slug }
        assertTrue("art1 should appear in the feed", idx1 >= 0)
        assertTrue("art2 should appear in the feed", idx2 >= 0)
        assertTrue("Article with more favorites must rank higher (earlier index)", idx1 < idx2)
        assertEquals("art1 should report 2 favorites", 2L, articles[idx1].favoritesCount)
    }

    @Test
    fun `popular feed returns empty list when no articles exist`() {
        appRule.http.createUser("empty_feed@popular_feed.com", "empty_feed_user")

        val response = appRule.http.get<ArticlesDTO>("/articles/feed/popular")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articles.isEmpty())
        assertEquals(0, response.body.articlesCount)
    }

    @Test
    fun `popular feed with limit and offset returns correct page`() {
        val http = appRule.http

        http.registerAndLogin("page_author1@test.com", "page_author1")
        val art1 = http.post<ArticleDTO>("/articles", ArticleDTO(
            Article(title = "Pagination Article Alpha", description = "desc alpha", body = "body alpha")
        )).body.article!!

        http.registerAndLogin("page_author2@test.com", "page_author2")
        val art2 = http.post<ArticleDTO>("/articles", ArticleDTO(
            Article(title = "Pagination Article Beta", description = "desc beta", body = "body beta")
        )).body.article!!

        // art1: 2 favorites, art2: 1 favorite
        http.registerAndLogin("page_voter1@test.com", "page_voter1")
        http.favoriteArticle(art1.slug!!)
        http.favoriteArticle(art2.slug!!)
        http.registerAndLogin("page_voter2@test.com", "page_voter2")
        http.favoriteArticle(art1.slug!!)

        val fullFeed = http.get<ArticlesDTO>("/articles/feed/popular", mapOf("limit" to 1000))
        val slugs = fullFeed.body.articles.map { it.slug }
        val rankArt1 = slugs.indexOf(art1.slug)
        val rankArt2 = slugs.indexOf(art2.slug)
        assertTrue("art1 (2 favorites) should rank before art2 (1 favorite)", rankArt1 < rankArt2)

        // limit=1 at art2's rank should return exactly art2
        val page = http.get<ArticlesDTO>("/articles/feed/popular", mapOf("limit" to 1, "offset" to rankArt2))
        assertEquals(HttpStatus.SC_OK, page.status)
        assertEquals("Page should contain exactly 1 article", 1, page.body.articles.size)
        assertEquals("Paginated result should be art2", art2.slug, page.body.articles[0].slug)
    }

    @Test
    fun `popular feed with invalid limit param returns 200 with default window`() {
        val http = appRule.http
        http.registerAndLogin("invalid_limit@test.com", "invalid_limit_user")

        val response = http.get<ArticlesDTO>("/articles/feed/popular", mapOf("limit" to "not-a-number"))
        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull("Articles list must not be null", response.body.articles)
        assertTrue("Should return at most the default 20 articles", response.body.articles.size <= 20)
    }
}
