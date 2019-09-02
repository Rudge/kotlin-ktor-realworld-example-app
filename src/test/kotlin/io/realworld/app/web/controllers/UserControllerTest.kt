package io.realworld.app.web.controllers

import io.realworld.app.domain.User
import io.realworld.app.domain.UserDTO
import io.realworld.app.web.rules.AppRule
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class UserControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

//    @Test
//    fun `invalid login without pass valid body`() {
//        val response = appRule.http.post<ErrorResponse>(
//            "/api/users/login",
//            UserDTO()
//        )
//
//        assertEquals(response.status, HttpStatus.SC_UNPROCESSABLE_ENTITY)
//        assertTrue(response.body.errors["body"]!!.contains("can't be empty or invalid"))
//    }

    @Test
    fun `success login with email and password`() {
        val email = "success_login@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "success_login")
        val userDTO = UserDTO(User(email = email, password = password))
        val response = appRule.http.post<UserDTO>("/users/login", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.email, userDTO.user?.email)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `success register user`() {
        val userDTO = UserDTO(
            User(
                email = "success_register@valid_email.com", password = "Test", username =
                "success_register"
            )
        )
        val response = appRule.http.post<UserDTO>("/users", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.username, userDTO.user?.username)
        assertEquals(response.body.user?.password, userDTO.user?.password)
    }

//    @Test
//    fun `invalid get current user without token`() {
//        val response = appRule.http.get<ErrorResponse>("/api/user")
//
//        assertEquals(response.status, HttpStatus.SC_FORBIDDEN)
//    }

    @Test
    fun `get current user by token`() {
        val email = "get_current@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "get_current")
        appRule.http.loginAndSetTokenHeader(email, password)
        val response = appRule.http.get<UserDTO>("/user")

        assertEquals(response.status, HttpStatus.SC_OK)
        assertNotNull(response.body.user?.username)
        assertNotNull(response.body.user?.password)
        assertNotNull(response.body.user?.token)
    }

    @Test
    fun `update user data`() {
        val email = "update_email@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "update_email")

        appRule.http.loginAndSetTokenHeader("update_email@valid_email.com", "Test")
        val userDTO = UserDTO(User(email = "update_user@update_test.com", password = "Test"))
        val response = appRule.http.put<UserDTO>("/user", userDTO)

        assertEquals(response.status, HttpStatus.SC_OK)
        assertEquals(response.body.user?.email, userDTO.user?.email)
    }
}
