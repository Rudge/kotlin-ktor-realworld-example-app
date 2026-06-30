package io.realworld.app.domain.repository

import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal object Tags : LongIdTable() {
    val name: Column<String> = varchar("name", 100).uniqueIndex()
}

class TagRepository {
    init {
        transaction {
            SchemaUtils.create(Tags)
        }
    }

    fun findAll(): List<String> = transaction {
        Tags.selectAll().map { it[Tags.name] }
    }

    /**
     * Returns the id of the tag named [name], creating the row if it does not exist yet.
     * Used when attaching tags to articles during create; avoids duplicate tag names.
     */
    fun findOrCreate(name: String): Long = transaction {
        Tags.select { Tags.name eq name }.firstOrNull()?.get(Tags.id)?.value
            ?: Tags.insertAndGetId { row -> row[Tags.name] = name }.value
    }
}
