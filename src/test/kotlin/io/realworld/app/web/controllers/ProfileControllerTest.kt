package io.realworld.app.web.controllers

import io.realworld.app.domain.ProfileDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
