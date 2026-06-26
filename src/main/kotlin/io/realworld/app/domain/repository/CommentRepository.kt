package io.realworld.app.domain.repository

import io.realworld.app.domain.Comment
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

internal object Comments : Table() {
    val id: Column<Long> = long("id").autoIncrement().primaryKey()
    val body: Column<String> = varchar("body", 2000)
    val authorId: Column<Long> = long("author_id")
    val articleSlug: Column<String> = varchar("article_slug", 255)
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
}

class CommentRepository {
    init {
        transaction {
            SchemaUtils.create(Comments)
        }
    }

    private fun toComment(row: ResultRow): Comment {
        val authorRow = Users.selectAll().firstOrNull { it[Users.id].value == row[Comments.authorId] }
        val author = authorRow?.let { Users.toDomain(it) }
        return Comment(
            id = row[Comments.id],
            body = row[Comments.body],
            createdAt = Date(row[Comments.createdAt]),
            updatedAt = Date(row[Comments.updatedAt]),
            author = author
        )
    }

    fun add(slug: String, authorId: Long, comment: Comment): Comment {
        val now = Date().time
        return transaction {
            val id = Comments.insert { row ->
                row[Comments.body] = comment.body
                row[Comments.authorId] = authorId
                row[Comments.articleSlug] = slug
                row[Comments.createdAt] = now
                row[Comments.updatedAt] = now
            }[Comments.id]
            val authorRow = Users.selectAll().firstOrNull { it[Users.id].value == authorId }
            Comment(
                id = id,
                body = comment.body,
                createdAt = Date(now),
                updatedAt = Date(now),
                author = authorRow?.let { Users.toDomain(it) }
            )
        }
    }

    fun findBySlug(slug: String): List<Comment> {
        return transaction {
            Comments.select { Comments.articleSlug eq slug }.map { toComment(it) }
        }
    }

    fun delete(id: Long, slug: String) {
        transaction {
            Comments.deleteWhere { (Comments.id eq id) and (Comments.articleSlug eq slug) }
        }
    }
}
