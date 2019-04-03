package io.realworld.app.domain.repository

import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
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
}
