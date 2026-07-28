package io.realworld.app.web.controllers

import io.realworld.app.domain.ProfileDTO
import io.realworld.app.domain.UserStatsDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class ProfileControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `get profile by username`() {
        val email = "get_profile@valid_email.com"
        val password = "Test"
        appRule.http.registerUser("celeb_get_profile@valid_email.com", password, "celeb_username")
        appRule.http.registerUser(email, password, "user_name_test")
        appRule.http.loginAndSetTokenHeader(email, password)

        val username = "celeb_username"
        val response = appRule.http.get<ProfileDTO>("/api/profiles/$username")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.profile?.username, username)
        assertFalse(response.body.profile?.following ?: true)
    }

    @Test
    fun `follow profile by username`() {
        val email = "follow_profile@valid_email.com"
        val password = "Test"
        appRule.http.registerUser("celeb_follow_profile@valid_email.com", password, "celeb_username")
        appRule.http.registerUser(email, password, "user_name_test")
        appRule.http.loginAndSetTokenHeader(email, password)

        val username = "celeb_username"
        val response = appRule.http.post<ProfileDTO>("/api/profiles/$username/follow")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.profile?.username, username)
        assertTrue(response.body.profile?.following ?: false)
    }

    @Test
    fun `unfollow profile by username`() {
        val email = "unfollow_profile@valid_email.com"
        val password = "Test"
        appRule.http.registerUser("celeb_unfollow_profile@valid_email.com", password, "celeb_username")
        appRule.http.registerUser(email, password, "user_name_test")
        appRule.http.loginAndSetTokenHeader(email, password)

        val username = "celeb_username"
        val response = appRule.http.deleteWithResponseBody<ProfileDTO>("/api/profiles/$username/follow")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.profile?.username, username)
        assertFalse(response.body.profile?.following ?: true)
    }

    @Test
    fun `get user stats without auth`() {
        val http = HttpUtil(appRule.port)
        val username = "testuser"
        val response = http.get<UserStatsDTO>("/api/profiles/$username/stats")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body)
        assertNotNull(response.body.stats)
        assertTrue(response.body.stats.articlesCount >= 0)
        assertTrue(response.body.stats.commentsCount >= 0)
        assertTrue(response.body.stats.favoritesCount >= 0)
    }

    @Test
    fun `get user stats with auth`() {
        val email = "stats_test@valid_email.com"
        val password = "Test"
        val username = "stats_user"
        appRule.http.registerUser(email, password, username)
        appRule.http.loginAndSetTokenHeader(email, password)

        val response = appRule.http.get<UserStatsDTO>("/api/profiles/$username/stats")

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
        val response = http.get<UserStatsDTO>("/api/profiles/anyuser/stats")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertNotNull(response.body.stats)
        // Verify the stats object has the expected fields
        assertEquals(0, response.body.stats.articlesCount)
        assertEquals(0, response.body.stats.commentsCount)
        assertEquals(0, response.body.stats.favoritesCount)
    }
}
