package io.realworld.app.domain

data class ProfileDTO(val profile: Profile?)

data class Profile(val username: String? = null,
                   val bio: String? = null,
                   val image: String? = null,
                   val following: Boolean = false)

data class UserStatsDTO(val stats: UserStats)

data class UserStats(
    val articlesCount: Int,
    val commentsCount: Int,
    val favoritesCount: Int
)