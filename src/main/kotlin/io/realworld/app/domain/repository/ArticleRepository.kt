package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.Profile
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.ext.toSlug
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

internal object Articles : Table() {
    val id: Column<Long> = long("id").autoIncrement()
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String> = varchar("description", 500)
    val body: Column<String> = text("body")
    val authorId: Column<Long> = long("author_id")
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

internal object ArticleFavorites : Table() {
    val article: Column<Long> = long("article")
    val user: Column<Long> = long("user")
    override val primaryKey = PrimaryKey(article, user)
}

internal object ArticleTags : Table() {
    val article: Column<Long> = long("article")
    val tag: Column<String> = varchar("tag", 100)
    override val primaryKey = PrimaryKey(article, tag)
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Articles, ArticleFavorites, ArticleTags)
        }
    }

    fun create(article: Article, authorId: Long): Article {
        val now = System.currentTimeMillis()
        val baseSlug = article.title!!.toSlug()
        val slug = transaction {
            var candidate = baseSlug
            var suffix = 1
            while (!Articles.select { Articles.slug eq candidate }.empty()) {
                candidate = "$baseSlug-$suffix"
                suffix++
            }
            candidate
        }
        transaction {
            val articleId = Articles.insert { row ->
                row[Articles.slug] = slug
                row[title] = article.title
                row[description] = article.description ?: ""
                row[body] = article.body
                row[Articles.authorId] = authorId
                row[createdAt] = now
                row[updatedAt] = now
            }[Articles.id]
            article.tagList.forEach { tagName ->
                if (Tags.select { Tags.name eq tagName }.empty()) {
                    Tags.insert { it[name] = tagName }
                }
                ArticleTags.insert { row ->
                    row[ArticleTags.article] = articleId
                    row[tag] = tagName
                }
            }
        }
        return getBySlug(slug, authorId) ?: throw NotFoundException("Article not found after creation.")
    }

    fun getBySlug(slug: String, requesterId: Long): Article? {
        return transaction {
            Articles.select { Articles.slug eq slug }
                .firstOrNull()
                ?.let { buildArticle(it, requesterId) }
        }
    }

    fun favorite(requesterId: Long, slug: String): Article {
        transaction {
            val articleId = articleIdBySlug(slug)
                ?: throw NotFoundException("Article not found to favorite.")
            val alreadyFavorited = ArticleFavorites
                .select { (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq requesterId) }
                .empty().not()
            if (!alreadyFavorited) {
                ArticleFavorites.insert { row ->
                    row[article] = articleId
                    row[user] = requesterId
                }
            }
        }
        return getBySlug(slug, requesterId)
            ?: throw NotFoundException("Article not found to favorite.")
    }

    fun unfavorite(requesterId: Long, slug: String): Article {
        transaction {
            val articleId = articleIdBySlug(slug)
                ?: throw NotFoundException("Article not found to unfavorite.")
            ArticleFavorites.deleteWhere {
                with(it) {
                    (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq requesterId)
                }
            }
        }
        return getBySlug(slug, requesterId)
            ?: throw NotFoundException("Article not found to unfavorite.")
    }

    fun countAll(): Int = transaction {
        Articles.selectAll().count().toInt()
    }

    fun findPopular(requesterId: Long, limit: Int, offset: Int): List<Article> {
        return transaction {
            val favoritesCount = ArticleFavorites.user.count()
            // Rank + paginate in SQL; only the page's ids/counts are materialized (no N+1, no full-table load).
            val rankedPage = Articles.join(
                ArticleFavorites, JoinType.LEFT,
                onColumn = Articles.id, otherColumn = ArticleFavorites.article
            )
                .slice(Articles.id, Articles.createdAt, favoritesCount)
                .selectAll()
                .groupBy(Articles.id, Articles.createdAt)
                .orderBy(favoritesCount to SortOrder.DESC, Articles.createdAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { it[Articles.id] to it[favoritesCount] }

            if (rankedPage.isEmpty()) return@transaction emptyList<Article>()

            val articleIds = rankedPage.map { it.first }
            val rowsById = Articles.select { Articles.id inList articleIds }
                .associateBy { it[Articles.id] }
            val authorIds = rowsById.values.map { it[Articles.authorId] }.distinct()

            val tagsByArticle: Map<Long, List<String>> = ArticleTags
                .select { ArticleTags.article inList articleIds }
                .groupBy({ it[ArticleTags.article] }, { it[ArticleTags.tag] })

            val authorsById: Map<Long, ResultRow> = Users
                .select { Users.id inList authorIds }
                .associateBy { it[Users.id] }

            val favoritedIds: Set<Long> = ArticleFavorites
                .select { (ArticleFavorites.user eq requesterId) and (ArticleFavorites.article inList articleIds) }
                .map { it[ArticleFavorites.article] }
                .toSet()

            val followingAuthorIds: Set<Long> = Follows
                .select { (Follows.follower eq requesterId) and (Follows.user inList authorIds) }
                .map { it[Follows.user] }
                .toSet()

            rankedPage.mapNotNull { (articleId, count) ->
                val row = rowsById[articleId] ?: return@mapNotNull null
                val authorId = row[Articles.authorId]
                val authorRow = authorsById[authorId]
                Article(
                    slug = row[Articles.slug],
                    title = row[Articles.title],
                    description = row[Articles.description],
                    body = row[Articles.body],
                    tagList = tagsByArticle[articleId] ?: emptyList(),
                    createdAt = Date(row[Articles.createdAt]),
                    updatedAt = Date(row[Articles.updatedAt]),
                    favorited = articleId in favoritedIds,
                    favoritesCount = count,
                    author = authorRow?.let {
                        Profile(
                            username = it[Users.username],
                            bio = it[Users.bio],
                            image = it[Users.image],
                            following = authorId in followingAuthorIds
                        )
                    }
                )
            }
        }
    }

    private fun articleIdBySlug(slug: String): Long? =
        Articles.select { Articles.slug eq slug }
            .firstOrNull()
            ?.get(Articles.id)

    private fun buildArticle(row: ResultRow, requesterId: Long): Article {
        val articleId = row[Articles.id]
        val authorId = row[Articles.authorId]
        val tags = ArticleTags.select { ArticleTags.article eq articleId }
            .map { it[ArticleTags.tag] }
        val favoritesCount = ArticleFavorites
            .select { ArticleFavorites.article eq articleId }
            .count()
        val favorited = ArticleFavorites
            .select { (ArticleFavorites.article eq articleId) and (ArticleFavorites.user eq requesterId) }
            .empty().not()
        val author = Users.select { Users.id eq authorId }
            .firstOrNull()
            ?.let {
                Profile(
                    username = it[Users.username],
                    bio = it[Users.bio],
                    image = it[Users.image],
                    following = Follows
                        .select { (Follows.follower eq requesterId) and (Follows.user eq authorId) }
                        .empty().not()
                )
            }
        return Article(
            slug = row[Articles.slug],
            title = row[Articles.title],
            description = row[Articles.description],
            body = row[Articles.body],
            tagList = tags,
            createdAt = Date(row[Articles.createdAt]),
            updatedAt = Date(row[Articles.updatedAt]),
            favorited = favorited,
            favoritesCount = favoritesCount,
            author = author
        )
    }
}
