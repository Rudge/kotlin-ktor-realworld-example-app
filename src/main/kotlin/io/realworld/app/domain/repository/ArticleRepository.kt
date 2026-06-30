package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.ext.toSlug
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 500)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val body: Column<String> = text("body")
    val author: Column<Long> = long("author_id")
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")
}

internal object ArticleTags : Table() {
    val article: Column<Long> = long("article_id").primaryKey()
    val tag: Column<Long> = long("tag_id").primaryKey()
}

internal object Favorites : Table() {
    val user: Column<Long> = long("user_id").primaryKey()
    val article: Column<Long> = long("article_id").primaryKey()
}

class ArticleRepository(
    private val userRepository: UserRepository,
    private val tagRepository: TagRepository
) {
    init {
        // Kept for `./gradlew run` on first startup; [DbConfig.setup] also creates these tables
        // so per-test databases work when this singleton init does not re-run.
        transaction {
            SchemaUtils.create(Articles, ArticleTags, Favorites)
        }
    }

    /**
     * Persists a new article for [authorEmail] and links [article.tagList] to tag rows.
     * Slug is derived from the title via [toSlug]. Returns the saved article with author populated.
     */
    fun create(authorEmail: String, article: Article): Article {
        val author = userRepository.findByEmail(authorEmail)
            ?: throw NotFoundException("Author not found.")
        val slug = (article.title ?: "").toSlug()
        val now = System.currentTimeMillis()

        return transaction {
            val articleId = Articles.insertAndGetId { row ->
                row[Articles.slug] = slug
                row[Articles.title] = article.title!!
                row[Articles.description] = article.description
                row[Articles.body] = article.body
                row[Articles.author] = author.id!!
                row[Articles.createdAt] = now
                row[Articles.updatedAt] = now
            }.value

            article.tagList.forEach { tagName ->
                val tagId = findOrCreateTagId(tagName)
                ArticleTags.insert {
                    it[ArticleTags.article] = articleId
                    it[ArticleTags.tag] = tagId
                }
            }

            val row = Articles.select { Articles.id eq articleId }.first()
            toDomain(row, author.id)
        }
    }

    /**
     * Records [userEmail] favoriting the article identified by [slug] (idempotent if already favorited).
     * Returns the article with updated favoritesCount and favorited flag for the viewer.
     */
    fun favorite(userEmail: String, slug: String): Article {
        val user = userRepository.findByEmail(userEmail)
            ?: throw NotFoundException("User not found.")

        return transaction {
            val row = Articles.select { Articles.slug eq slug }.firstOrNull()
                ?: throw NotFoundException("Article not found.")
            val articleId = row[Articles.id].value

            val exists = Favorites.select {
                (Favorites.user eq user.id!!) and (Favorites.article eq articleId)
            }.count() > 0
            if (!exists) {
                Favorites.insert {
                    it[Favorites.user] = user.id!!
                    it[Favorites.article] = articleId
                }
            }

            toDomain(row, user.id)
        }
    }

    /**
     * Returns all articles sorted by favorites count (desc), then slug, with pagination.
     *
     * @param viewerEmail authenticated user email; used to set each article's favorited flag
     * @param limit max rows in the page (validated upstream in [ArticleService])
     * @param offset rows to skip before [limit]
     * @return pair of (page of articles, total count before pagination)
     */
    fun findPopular(viewerEmail: String?, limit: Int, offset: Int): Pair<List<Article>, Int> {
        val viewer = viewerEmail?.let { userRepository.findByEmail(it) }

        return transaction {
            val favoriteCounts = Favorites.selectAll()
                .groupBy { it[Favorites.article] }
                .mapValues { (_, rows) -> rows.size.toLong() }

            val sorted = Articles.selectAll()
                .map { row ->
                    val articleId = row[Articles.id].value
                    toDomain(row, viewer?.id, favoriteCounts[articleId] ?: 0)
                }
                .sortedWith(compareByDescending<Article> { it.favoritesCount }.thenBy { it.slug })

            val total = sorted.size
            val page = sorted.drop(offset).take(limit)
            page to total
        }
    }

    /** Looks up a tag by [name] in the current transaction, inserting it if missing. */
    private fun findOrCreateTagId(name: String): Long =
        Tags.select { Tags.name eq name }.firstOrNull()?.get(Tags.id)?.value
            ?: Tags.insertAndGetId { row -> row[Tags.name] = name }.value

    /** Maps a DB row to [Article], optionally reusing a precomputed [favoritesCount] for sorting. */
    private fun toDomain(row: ResultRow, viewerId: Long?, favoritesCount: Long? = null): Article {
        val articleId = row[Articles.id].value
        val author = Users.select { Users.id eq row[Articles.author] }
            .first().let { Users.toDomain(it) }
        val tags = ArticleTags.join(Tags, JoinType.INNER, ArticleTags.tag, Tags.id)
            .select { ArticleTags.article eq articleId }
            .map { it[Tags.name] }
        val count = favoritesCount ?: Favorites.select { Favorites.article eq articleId }.count()
        val favorited = viewerId?.let { viewer ->
            Favorites.select {
                (Favorites.user eq viewer) and (Favorites.article eq articleId)
            }.count() > 0
        } ?: false

        return Article(
            slug = row[Articles.slug],
            title = row[Articles.title],
            description = row[Articles.description],
            body = row[Articles.body],
            tagList = tags,
            createdAt = java.util.Date(row[Articles.createdAt]),
            updatedAt = java.util.Date(row[Articles.updatedAt]),
            favorited = favorited,
            favoritesCount = count.toLong(),
            author = author.copy(password = null, token = null)
        )
    }
}
