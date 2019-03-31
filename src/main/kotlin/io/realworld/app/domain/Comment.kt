package io.realworld.app.domain

import java.util.*

data class CommentDTO(val comment: Comment?)
data class CommentsDTO(val comments: List<Comment>)

data class Comment(val id: Long? = null,
                   val createdAt: Date? = null,
                   val updatedAt: Date? = null,
                   val body: String,
                   val author: User? = null)