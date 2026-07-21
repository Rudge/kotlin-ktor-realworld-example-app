package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.Profile
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String> = varchar("description", 1000)
    val body: Column<String> = varchar("body", 10000)
    val authorId: Column<Long> = long("author_id")
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
}

internal object ArticleTags : Table() {
    val articleId: Column<Long> = long("article_id")
    val tag: Column<String> = varchar("tag", 100)
    override val primaryKey = PrimaryKey(articleId, tag)
}

internal object Favorites : Table() {
    val articleId: Column<Long> = long("article_id")
    val userId: Column<Long> = long("user_id")
    override val primaryKey = PrimaryKey(articleId, userId)
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Articles)
            SchemaUtils.create(ArticleTags)
            SchemaUtils.create(Favorites)
        }
    }

    fun countAll(): Int = transaction {
        Articles.selectAll().count().toInt()
    }

    /**
     * Returns a page of articles ordered by favorite count (most favorited first), tie-broken by
     * newest first. The ordering/grouping is done in Kotlin rather than SQL to keep the query
     * surface small; this is fine at the scale this demo app runs at, but would want to become a
     * `GROUP BY ... ORDER BY count(*)` query if the article table ever grew large.
     */
    fun findPopular(limit: Int, offset: Int, currentUserId: Long?): List<Article> = transaction {
        val favoriteRows = Favorites.selectAll().map { it[Favorites.articleId] to it[Favorites.userId] }
        val favoriteCountByArticle = favoriteRows.groupingBy { it.first }.eachCount()
        val favoritedArticleIds = if (currentUserId != null) {
            favoriteRows.filter { it.second == currentUserId }.map { it.first }.toSet()
        } else {
            emptySet()
        }

        val sortedRows = Articles.selectAll().toList().sortedWith(
            compareByDescending<ResultRow> { favoriteCountByArticle[it[Articles.id].value] ?: 0 }
                .thenByDescending { it[Articles.createdAt] }
        )
        val pageRows = sortedRows.drop(offset).take(limit)
        val pageArticleIds = pageRows.map { it[Articles.id].value }.toSet()

        val tagsByArticle = ArticleTags.selectAll()
            .filter { it[ArticleTags.articleId] in pageArticleIds }
            .groupBy({ it[ArticleTags.articleId] }, { it[ArticleTags.tag] })

        val authorIds = pageRows.map { it[Articles.authorId] }.toSet()
        val usersById = Users.selectAll()
            .filter { it[Users.id].value in authorIds }
            .associateBy({ it[Users.id].value }, { Users.toDomain(it) })

        val followedIds = if (currentUserId != null) {
            Follows.selectAll()
                .filter { it[Follows.follower] == currentUserId }
                .map { it[Follows.user] }
                .toSet()
        } else {
            emptySet()
        }

        pageRows.map { row ->
            val articleId = row[Articles.id].value
            val author = usersById[row[Articles.authorId]]
            Article(
                slug = row[Articles.slug],
                title = row[Articles.title],
                description = row[Articles.description],
                body = row[Articles.body],
                tagList = tagsByArticle[articleId] ?: listOf(),
                createdAt = Date(row[Articles.createdAt]),
                updatedAt = Date(row[Articles.updatedAt]),
                favorited = articleId in favoritedArticleIds,
                favoritesCount = (favoriteCountByArticle[articleId] ?: 0).toLong(),
                author = author?.let {
                    Profile(
                        username = it.username,
                        bio = it.bio,
                        image = it.image,
                        following = followedIds.contains(it.id ?: -1L)
                    )
                }
            )
        }
    }
}
