package io.realworld.app.domain

data class UserDTO(val user: User? = null)

data class User(val id: Long? = null,
                val email: String,
                val token: String? = null,
                val username: String? = null,
                val password: String? = null,
                val bio: String? = null,
                val image: String? = null)