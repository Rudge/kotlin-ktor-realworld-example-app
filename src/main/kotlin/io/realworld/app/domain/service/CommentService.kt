package io.realworld.app.domain.service

import io.realworld.app.domain.Comment
import io.realworld.app.domain.repository.CommentRepository
import io.realworld.app.domain.repository.UserRepository

class CommentService(
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository
) {
    fun add(slug: String, email: String, comment: Comment): Comment {
        val user = userRepository.findByEmail(email) ?: error("User not found: $email")
        return commentRepository.add(slug, user.id!!, comment)
    }

    fun findBySlug(slug: String): List<Comment> = commentRepository.findBySlug(slug)

    fun delete(id: Long, slug: String) = commentRepository.delete(id, slug)
}
