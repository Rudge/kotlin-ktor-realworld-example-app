package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.Profile
import io.realworld.app.domain.exceptions.NotFoundException
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.wrapAsExpression
import java.time.Instant
import java.util.Date

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String?> = varchar("description", 500).nullable()
    val body: Column<String> = text("body")
    val createdAt: Column<Instant> = timestamp("created_at")
    val updatedAt: Column<Instant> = timestamp("updated_at")
    val author: Column<Long> = long("author").index()
}

internal object ArticleFavorites : Table("article_favorites") {
    val article: Column<Long> = long("article")
    val user: Column<Long> = long("user_id")
    override val primaryKey = PrimaryKey(article, user)
}

internal object ArticleTags : Table("article_tags") {
    val article: Column<Long> = long("article")
    val tag: Column<Long> = long("tag")
    override val primaryKey = PrimaryKey(article, tag)
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Articles, ArticleFavorites, ArticleTags, Tags)
        }
    }

    /**
     * Ranked by favorite count, then newest first. The secondary key matters: without a
     * deterministic tie-break, two pages of an equally-favorited set can repeat or skip rows.
     */
    fun findPopular(viewerId: Long, limit: Int, offset: Int): List<Article> = transaction {
        val favoritesCount = favoritesCountExpression()
        val rows = Articles
            .join(Users, JoinType.INNER, additionalConstraint = { Articles.author eq Users.id })
            .slice(Articles.columns + Users.columns + favoritesCount)
            .selectAll()
            .orderBy(
                favoritesCount to SortOrder.DESC,
                Articles.createdAt to SortOrder.DESC,
                Articles.id to SortOrder.DESC
            )
            .limit(limit, offset.toLong())
            .toList()

        // Three bounded lookups for the page rather than a subquery per row or an N+1 per article.
        val articleIds = rows.map { it[Articles.id].value }
        val authorIds = rows.map { it[Articles.author] }
        val tagsByArticle = tagsFor(articleIds)
        val favoritedByViewer = favoritedBy(viewerId, articleIds)
        val followedByViewer = followedBy(viewerId, authorIds)

        rows.map { row ->
            val id = row[Articles.id].value
            toDomain(
                row = row,
                favoritesCount = favoritesCount,
                tagList = tagsByArticle[id].orEmpty(),
                favorited = id in favoritedByViewer,
                following = row[Articles.author] in followedByViewer
            )
        }
    }

    fun count(): Long = transaction { Articles.selectAll().count() }

    fun create(article: Article, authorId: Long): String = transaction {
        val slug = uniqueSlug(article.title!!.toSlug())
        val now = Instant.now()
        val articleId = Articles.insert { row ->
            row[Articles.slug] = slug
            row[title] = article.title
            row[description] = article.description
            row[body] = article.body
            row[createdAt] = now
            row[updatedAt] = now
            row[author] = authorId
        } get Articles.id

        article.tagList.filter { it.isNotBlank() }.distinct().forEach { name ->
            val tagId = Tags.select { Tags.name eq name }.map { it[Tags.id].value }.firstOrNull()
                ?: (Tags.insert { it[Tags.name] = name } get Tags.id).value
            ArticleTags.insert { row ->
                row[ArticleTags.article] = articleId.value
                row[tag] = tagId
            }
        }
        slug
    }

    /**
     * Idempotent: favoriting twice leaves a single row and the same count. Checked explicitly
     * rather than with insertIgnore, which H2 only supports in MySQL compatibility mode.
     */
    fun favorite(slug: String, userId: Long) = transaction {
        val articleId = articleIdOf(slug)
        val alreadyFavorited = ArticleFavorites
            .select { (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq userId) }
            .limit(1)
            .any()
        if (!alreadyFavorited) {
            ArticleFavorites.insert { row ->
                row[article] = articleId
                row[user] = userId
            }
        }
        Unit
    }

    fun unfavorite(slug: String, userId: Long) = transaction {
        val articleId = articleIdOf(slug)
        ArticleFavorites.deleteWhere {
            (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq userId)
        }
        Unit
    }

    fun findBySlug(slug: String, viewerId: Long): Article = transaction {
        val favoritesCount = favoritesCountExpression()
        val row = Articles
            .join(Users, JoinType.INNER, additionalConstraint = { Articles.author eq Users.id })
            .slice(Articles.columns + Users.columns + favoritesCount)
            .select { Articles.slug eq slug }
            .firstOrNull() ?: throw NotFoundException("Article not found by slug '$slug'.")

        val id = row[Articles.id].value
        toDomain(
            row = row,
            favoritesCount = favoritesCount,
            tagList = tagsFor(listOf(id))[id].orEmpty(),
            favorited = id in favoritedBy(viewerId, listOf(id)),
            following = row[Articles.author] in followedBy(viewerId, listOf(row[Articles.author]))
        )
    }

    // Correlated subquery rather than GROUP BY: an inner join to the favorites table would
    // drop articles nobody has favorited, which still belong in the ranking (last).
    // Built inside the transaction — Exposed needs one in context to render an expression.
    private fun favoritesCountExpression(): Expression<Long?> = wrapAsExpression<Long>(
        ArticleFavorites
            .slice(ArticleFavorites.user.count())
            .select { ArticleFavorites.article eq Articles.id }
    )

    private fun articleIdOf(slug: String): Long =
        Articles.select { Articles.slug eq slug }
            .map { it[Articles.id].value }
            .firstOrNull() ?: throw NotFoundException("Article not found by slug '$slug'.")

    private fun uniqueSlug(base: String): String {
        var candidate = base
        var suffix = 1
        while (Articles.select { Articles.slug eq candidate }.limit(1).any()) {
            candidate = "$base-${++suffix}"
        }
        return candidate
    }

    private fun tagsFor(articleIds: List<Long>): Map<Long, List<String>> {
        if (articleIds.isEmpty()) return emptyMap()
        return ArticleTags
            .join(Tags, JoinType.INNER, additionalConstraint = { ArticleTags.tag eq Tags.id })
            .slice(ArticleTags.article, Tags.name)
            .select { ArticleTags.article inList articleIds }
            .groupBy({ it[ArticleTags.article] }, { it[Tags.name] })
            .mapValues { (_, names) -> names.sorted() }
    }

    private fun favoritedBy(viewerId: Long, articleIds: List<Long>): Set<Long> {
        if (articleIds.isEmpty()) return emptySet()
        return ArticleFavorites
            .select { (ArticleFavorites.user eq viewerId) and (ArticleFavorites.article inList articleIds) }
            .mapTo(mutableSetOf()) { it[ArticleFavorites.article] }
    }

    private fun followedBy(viewerId: Long, authorIds: List<Long>): Set<Long> {
        if (authorIds.isEmpty()) return emptySet()
        return Follows
            .select { (Follows.follower eq viewerId) and (Follows.user inList authorIds) }
            .mapTo(mutableSetOf()) { it[Follows.user] }
    }

    private fun toDomain(
        row: ResultRow,
        favoritesCount: Expression<Long?>,
        tagList: List<String>,
        favorited: Boolean,
        following: Boolean
    ) = Article(
        slug = row[Articles.slug],
        title = row[Articles.title],
        description = row[Articles.description],
        body = row[Articles.body],
        tagList = tagList,
        createdAt = Date.from(row[Articles.createdAt]),
        updatedAt = Date.from(row[Articles.updatedAt]),
        favorited = favorited,
        favoritesCount = row[favoritesCount] ?: 0L,
        // Profile, not User: the author payload cannot carry credentials by construction.
        author = Profile(
            username = row[Users.username],
            bio = row[Users.bio],
            image = row[Users.image],
            following = following
        )
    )
}

private val NON_SLUG_CHARS = Regex("[^a-z0-9]+")

private fun String.toSlug() = lowercase().replace(NON_SLUG_CHARS, "-").trim('-')
