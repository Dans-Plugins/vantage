package uk.co.renbinden.vantage.auth

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import uk.co.renbinden.vantage.jooq.Tables.PANEL_USER
import uk.co.renbinden.vantage.jooq.tables.records.PanelUserRecord
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserId
import uk.co.renbinden.vantage.user.UserRepository
import uk.co.renbinden.vantage.user.UserStatus

/**
 * A [UserRepository] backed by jOOQ's [MockConnection] instead of PostgreSQL.
 *
 * Supports every [UserRepository] query:
 * - the single-user lookups `getUser(UserId)` and `getUser(String)`, by matching the query's one
 *   binding against each stored user's id or username;
 * - `getUsers(amount, offset)`, by sorting [users] by username and applying the offset and limit;
 * - `getUserCount()`, by counting [users];
 * - `upsert(User)`, by inserting a user whose id is not stored with version 1, or replacing a
 *   stored user whose version matches with version + 1, mirroring the `on conflict ... where`
 *   clause. A version mismatch returns no row, as PostgreSQL would.
 *
 * Replace the contents of [users] to change what later queries return.
 */
class InMemoryUserRepository {

    val users = mutableListOf<User>()

    val repository = UserRepository(DSL.using(MockConnection { context ->
        val sql = context.sql()
        val bindings = context.bindings()
        when {
            sql.startsWith("insert") -> arrayOf(userResult(upsert(bindings)))
            sql.contains("count(*)") -> {
                val result = DSL.using(POSTGRES).newResult(DSL.count())
                result.add(DSL.using(POSTGRES).newRecord(DSL.count()).values(users.size))
                arrayOf(MockResult(1, result))
            }
            // The PostgreSQL dialect renders offset/limit as "offset ? rows fetch next ? rows only".
            sql.contains(" fetch next ") -> {
                val (offset, limit) = bindings.map { (it as Number).toInt() }
                arrayOf(userResult(users.sortedBy { it.username }.drop(offset).take(limit)))
            }
            else -> {
                val binding = bindings.single()
                arrayOf(userResult(users.filter { it.id.value == binding || it.username == binding }))
            }
        }
    }, POSTGRES))

    private fun upsert(bindings: Array<Any?>): List<User> {
        val id = UserId(bindings[0] as String)
        val expectedVersion = (bindings[12] as Number).toInt()
        val user = User(
            id,
            1,
            bindings[2] as String,
            bindings[3] as ByteArray,
            bindings[4] as ByteArray,
            UserStatus.valueOf(bindings[5] as String)
        )
        val index = users.indexOfFirst { it.id == id }
        if (index == -1) {
            users.add(user)
            return listOf(user)
        }
        if (users[index].version != expectedVersion) return emptyList()
        val updatedUser = user.copy(version = expectedVersion + 1)
        users[index] = updatedUser
        return listOf(updatedUser)
    }

    private fun userResult(matches: List<User>): MockResult {
        val result = DSL.using(POSTGRES).newResult(PANEL_USER)
        matches.forEach { user -> result.add(toRecord(user)) }
        return MockResult(result.size, result)
    }

    private fun toRecord(user: User): PanelUserRecord = DSL.using(POSTGRES).newRecord(PANEL_USER).apply {
        set(PANEL_USER.ID, user.id.value)
        set(PANEL_USER.VERSION, user.version)
        set(PANEL_USER.USERNAME, user.username)
        set(PANEL_USER.PASSWORD_HASH, user.passwordHash)
        set(PANEL_USER.PASSWORD_SALT, user.passwordSalt)
        set(PANEL_USER.STATUS, user.status.name)
    }

}
