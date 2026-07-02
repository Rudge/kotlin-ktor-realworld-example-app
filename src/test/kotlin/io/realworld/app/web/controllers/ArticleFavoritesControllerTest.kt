package io.realworld.app.web.controllers

import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleFavoritesControllerTest {

    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `create article persists and appears in popular feed`() {
        val article = Article(
            title = "Favorites Feed Test Article",
            description = "Testing article creation",
            body = "Article body content",
            tagList = listOf("feed-test")
        )
        val createResponse = appRule.http.createArticleAs(
            article,
            email = "feed_test_author@test.com",
            username = "feed_test_author"
        )
        assertEquals(HttpStatus.SC_OK, createResponse.status)
        assertNotNull(createResponse.body.article)
        val slug = createResponse.body.article?.slug
        assertNotNull(slug)

        // Favorite it so it appears in the popular feed
        appRule.http.favoriteArticle(slug!!)

        val feedResponse = appRule.http.get<ArticlesDTO>("/articles/feed/popular")
        assertEquals(HttpStatus.SC_OK, feedResponse.status)
        val found = feedResponse.body.articles.any { it.slug == slug }
        assertTrue("Created article should appear in the popular feed", found)
    }

    @Test
    fun `favorite article increments favoritesCount and sets favorited true`() {
        val article = Article(
            title = "Favorite Count Test",
            description = "Testing favorite count",
            body = "Article body",
            tagList = listOf("fav-count")
        )
        val createResponse = appRule.http.createArticleAs(
            article,
            email = "fav_count_author@test.com",
            username = "fav_count_author"
        )
        val slug = createResponse.body.article?.slug!!

        val favResponse = appRule.http.favoriteArticle(slug)
        assertEquals(HttpStatus.SC_OK, favResponse.status)
        assertNotNull(favResponse.body.article)
        assertTrue("favorited should be true after favoriting", favResponse.body.article!!.favorited)
        assertEquals(1L, favResponse.body.article!!.favoritesCount)
    }

    @Test
    fun `unfavorite article decrements favoritesCount`() {
        val article = Article(
            title = "Unfavorite Count Test",
            description = "Testing unfavorite",
            body = "Article body",
            tagList = listOf("unfav-count")
        )
        val createResponse = appRule.http.createArticleAs(
            article,
            email = "unfav_count_author@test.com",
            username = "unfav_count_author"
        )
        val slug = createResponse.body.article?.slug!!

        appRule.http.favoriteArticle(slug)
        val unfavResponse = appRule.http.unfavoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, unfavResponse.status)
        assertNotNull(unfavResponse.body.article)
        assertFalse("favorited should be false after unfavoriting", unfavResponse.body.article!!.favorited)
        assertEquals(0L, unfavResponse.body.article!!.favoritesCount)
    }

    @Test
    fun `repeat favorite by same user does not double-count`() {
        val article = Article(
            title = "Idempotent Favorite Test",
            description = "Testing idempotent favorite",
            body = "Article body",
            tagList = listOf("idempotent")
        )
        val createResponse = appRule.http.createArticleAs(
            article,
            email = "idempotent_author@test.com",
            username = "idempotent_author"
        )
        val slug = createResponse.body.article?.slug!!

        // Favorite twice as the same user
        appRule.http.favoriteArticle(slug)
        val secondFavResponse = appRule.http.favoriteArticle(slug)

        assertEquals(HttpStatus.SC_OK, secondFavResponse.status)
        assertNotNull(secondFavResponse.body.article)
        assertEquals(
            "Repeat favorite by same user should not double-count",
            1L,
            secondFavResponse.body.article!!.favoritesCount
        )
    }

    @Test
    fun `favorite non-existent slug returns 404`() {
        appRule.http.registerAndLogin("fav_404_user@test.com", "fav_404_user")
        val response = appRule.http.postRaw("/articles/non-existent-slug-xyz/favorite")
        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
    }

    @Test
    fun `unfavorite non-existent slug returns 404`() {
        appRule.http.registerAndLogin("unfav_404_user@test.com", "unfav_404_user")
        val response = appRule.http.delete("/articles/non-existent-slug-xyz/favorite")
        assertEquals(HttpStatus.SC_NOT_FOUND, response.status)
    }

    @Test
    fun `create article with blank title returns 422`() {
        appRule.http.registerAndLogin("blank_title_user@test.com", "blank_title_user")
        val article = Article(
            title = "",
            description = "Some description",
            body = "Some body"
        )
        val response = appRule.http.postRaw("/articles", ArticleDTO(article))
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
    }
}
