package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
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
import org.jetbrains.exposed.sql.update
import java.util.Date

internal object Articles : Table() {
    val id: Column<Long> = long("id").autoIncrement().primaryKey()
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
            SchemaUtils.create(Articles, ArticleTags, Favorites)
        }
    }

    private fun toArticle(row: ResultRow, currentUserId: Long?): Article {
        val articleId = row[Articles.id]
        val authorRow = Users.selectAll().firstOrNull { it[Users.id].value == row[Articles.authorId] }
        val author = authorRow?.let { Users.toDomain(it) }
        val tagList = ArticleTags.select { ArticleTags.articleId eq articleId }.map { it[ArticleTags.tag] }
        val favoritesCount = Favorites.select { Favorites.articleId eq articleId }.count()
        val favorited = currentUserId != null &&
            Favorites.select { (Favorites.articleId eq articleId) and (Favorites.userId eq currentUserId) }.count() > 0
        return Article(
            slug = row[Articles.slug],
            title = row[Articles.title],
            description = row[Articles.description],
            body = row[Articles.body],
            tagList = tagList,
            createdAt = Date(row[Articles.createdAt]),
            updatedAt = Date(row[Articles.updatedAt]),
            favorited = favorited,
            favoritesCount = favoritesCount,
            author = author
        )
    }

    fun create(article: Article, authorId: Long): Article {
        val now = Date().time
        return transaction {
            val articleId = Articles.insert { row ->
                row[Articles.slug] = article.slug!!
                row[Articles.title] = article.title!!
                row[Articles.description] = article.description!!
                row[Articles.body] = article.body
                row[Articles.authorId] = authorId
                row[Articles.createdAt] = now
                row[Articles.updatedAt] = now
            }[Articles.id]
            article.tagList.forEach { tag ->
                ArticleTags.insert { row ->
                    row[ArticleTags.articleId] = articleId
                    row[ArticleTags.tag] = tag
                }
                if (Tags.select { Tags.name eq tag }.count() == 0L) {
                    Tags.insert { row -> row[Tags.name] = tag }
                }
            }
            toArticle(Articles.select { Articles.id eq articleId }.first(), authorId)
        }
    }

    fun findBySlug(slug: String, currentUserId: Long? = null): Article? {
        return transaction {
            Articles.select { Articles.slug eq slug }.firstOrNull()?.let { toArticle(it, currentUserId) }
        }
    }

    fun findBy(tag: String?, author: String?, favorited: String?, currentUserId: Long?, limit: Int, offset: Int): List<Article> {
        return transaction {
            var filtered = Articles.selectAll().toList().map { Triple(it[Articles.id], it[Articles.authorId], it) }

            if (tag != null) {
                val taggedIds = ArticleTags.select { ArticleTags.tag eq tag }.map { it[ArticleTags.articleId] }.toSet()
                filtered = filtered.filter { (id, _, _) -> id in taggedIds }
            }
            if (author != null) {
                val authorId = Users.select { Users.username eq author }.firstOrNull()?.get(Users.id)?.value
                filtered = if (authorId != null) filtered.filter { (_, aId, _) -> aId == authorId } else listOf()
            }
            if (favorited != null) {
                val favUser = Users.select { Users.username eq favorited }.firstOrNull()
                if (favUser != null) {
                    val favUserId = favUser[Users.id].value
                    val favIds = Favorites.select { Favorites.userId eq favUserId }.map { it[Favorites.articleId] }.toSet()
                    filtered = filtered.filter { (id, _, _) -> id in favIds }
                } else {
                    filtered = listOf()
                }
            }
            filtered.drop(offset).take(limit).map { (_, _, row) -> toArticle(row, currentUserId) }
        }
    }

    fun findPopular(currentUserId: Long?, limit: Int, offset: Int): List<Article> {
        return transaction {
            Articles.selectAll().toList()
                .sortedByDescending { row ->
                    Favorites.select { Favorites.articleId eq row[Articles.id] }.count()
                }
                .drop(offset)
                .take(limit)
                .map { toArticle(it, currentUserId) }
        }
    }

    fun findFeed(currentUserId: Long?, limit: Int, offset: Int): List<Article> {
        if (currentUserId == null) return listOf()
        return transaction {
            val followedIds = Follows.select { Follows.follower eq currentUserId }.map { it[Follows.user] }.toSet()
            if (followedIds.isEmpty()) return@transaction listOf()
            Articles.selectAll()
                .filter { it[Articles.authorId] in followedIds }
                .drop(offset)
                .take(limit)
                .map { toArticle(it, currentUserId) }
        }
    }

    fun update(slug: String, article: Article, currentUserId: Long?): Article? {
        return transaction {
            Articles.update({ Articles.slug eq slug }) { row ->
                if (article.title != null) row[Articles.title] = article.title
                if (article.description != null) row[Articles.description] = article.description
                row[Articles.body] = article.body
                row[Articles.updatedAt] = Date().time
            }
            if (article.tagList.isNotEmpty()) {
                val existingRow = Articles.select { Articles.slug eq slug }.firstOrNull()
                if (existingRow != null) {
                    val articleId = existingRow[Articles.id]
                    ArticleTags.deleteWhere { ArticleTags.articleId eq articleId }
                    article.tagList.forEach { tag ->
                        ArticleTags.insert { row ->
                            row[ArticleTags.articleId] = articleId
                            row[ArticleTags.tag] = tag
                        }
                    }
                }
            }
            Articles.select { Articles.slug eq slug }.firstOrNull()?.let { toArticle(it, currentUserId) }
        }
    }

    fun delete(slug: String) {
        transaction {
            val articleId = Articles.select { Articles.slug eq slug }.firstOrNull()?.get(Articles.id)
            if (articleId != null) {
                ArticleTags.deleteWhere { ArticleTags.articleId eq articleId }
                Favorites.deleteWhere { Favorites.articleId eq articleId }
            }
            Articles.deleteWhere { Articles.slug eq slug }
        }
    }

    fun favorite(slug: String, userId: Long): Article? {
        return transaction {
            val articleId = Articles.select { Articles.slug eq slug }.firstOrNull()?.get(Articles.id)
                ?: return@transaction null
            val exists = Favorites.select { (Favorites.articleId eq articleId) and (Favorites.userId eq userId) }.count() > 0
            if (!exists) {
                Favorites.insert { row ->
                    row[Favorites.articleId] = articleId
                    row[Favorites.userId] = userId
                }
            }
            toArticle(Articles.select { Articles.slug eq slug }.first(), userId)
        }
    }

    fun unfavorite(slug: String, userId: Long): Article? {
        return transaction {
            val articleId = Articles.select { Articles.slug eq slug }.firstOrNull()?.get(Articles.id)
                ?: return@transaction null
            Favorites.deleteWhere { (Favorites.articleId eq articleId) and (Favorites.userId eq userId) }
            toArticle(Articles.select { Articles.slug eq slug }.first(), userId)
        }
    }
}
