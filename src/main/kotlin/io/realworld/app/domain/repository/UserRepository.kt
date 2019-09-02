package io.realworld.app.domain.repository

import io.realworld.app.domain.User
import io.realworld.app.domain.exceptions.NotFoundException
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

internal object Users : LongIdTable() {
    val email: Column<String> = varchar("email", 200).uniqueIndex()
    val username: Column<String> = varchar("username", 100).uniqueIndex()
    val password: Column<String> = varchar("password", 150)
    val bio: Column<String?> = varchar("bio", 1000).nullable()
    val image: Column<String?> = varchar("image", 255).nullable()

    fun toDomain(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[email],
            username = row[username],
            password = row[password],
            bio = row[bio],
            image = row[image]
        )
    }
}

internal object Follows : Table() {
    val user: Column<Long> = long("user").primaryKey()
    val follower: Column<Long> = long("user_follower").primaryKey()
}

class UserRepository {
    init {
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Follows)
        }
    }

    fun findByEmail(email: String): User? {
        return transaction {
            Users.select { Users.email eq email }
                .map { Users.toDomain(it) }
                .firstOrNull()
        }
    }

    fun findByUsername(username: String): User? {
        return transaction {
            Users.select { Users.username eq username }
                .map { Users.toDomain(it) }
                .firstOrNull()
        }
    }

    fun create(user: User): Long? {
        return transaction {
            Users.insertAndGetId { row ->
                row[email] = user.email
                row[username] = user.username!!
                row[password] = user.password!!
                row[bio] = user.bio
                row[image] = user.image
            }.value
        }
    }

    fun update(email: String, user: User): User? {
        transaction {
            Users.update({ Users.email eq email }) { row ->
                row[Users.email] = user.email
                if (user.username != null) {
                    row[username] = user.username
                }
                if (user.password != null) {
                    row[password] = user.password
                }
                if (user.bio != null) {
                    row[bio] = user.bio
                }
                if (user.image != null) {
                    row[image] = user.image
                }
            }
        }
        return findByEmail(user.email)
    }

    fun findIsFollowUser(email: String, userIdToFollow: Long): Boolean {
        return transaction {
            Users.join(Follows, JoinType.INNER,
                additionalConstraint = {
                    Follows.user eq Users.id and (Follows.follower eq userIdToFollow)
                })
                .select {
                    Users.email eq email
                }
                .count() > 0
        }
    }

    fun follow(email: String, usernameToFollow: String): User {
        val user = findByEmail(email) ?: throw NotFoundException("Email not found to follow")
        val userToFollow = findByUsername(usernameToFollow) ?: throw NotFoundException("Username not found to follow")
        transaction {
            Follows.insert { row ->
                row[Follows.user] = userToFollow.id!!
                row[follower] = user.id!!
            }
        }
        return userToFollow
    }

    fun unfollow(email: String, usernameToUnFollow: String): User {
        val user = findByEmail(email) ?: throw NotFoundException("Email not found to unfollow")
        val userToUnfollow = findByUsername(usernameToUnFollow)
            ?: throw NotFoundException("Username not found to unfollow")
        transaction {
            Follows.deleteWhere {
                Follows.user eq user.id!! and (Follows.follower eq userToUnfollow.id!!)
            }
        }
        return userToUnfollow
    }
}
