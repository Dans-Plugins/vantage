package uk.co.renbinden.vantage.auth

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import uk.co.renbinden.vantage.jooq.Tables.PANEL_USER
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserRepository

/**
 * A [UserRepository] backed by jOOQ's [MockConnection] instead of PostgreSQL.
 *
 * Supports the two single-user lookups the auth classes make — `getUser(UserId)` and
 * `getUser(String)` — by matching the query's one binding against each stored user's id or
 * username. Replace the contents of [users] to change what later lookups return.
 */
class InMemoryUserRepository {

    val users = mutableListOf<User>()

    val repository = UserRepository(DSL.using(MockConnection { context ->
        val binding = context.bindings().single()
        val result = DSL.using(POSTGRES).newResult(PANEL_USER)
        users.filter { it.id.value == binding || it.username == binding }.forEach { user ->
            result.add(DSL.using(POSTGRES).newRecord(PANEL_USER).apply {
                set(PANEL_USER.ID, user.id.value)
                set(PANEL_USER.VERSION, user.version)
                set(PANEL_USER.USERNAME, user.username)
                set(PANEL_USER.PASSWORD_HASH, user.passwordHash)
                set(PANEL_USER.PASSWORD_SALT, user.passwordSalt)
                set(PANEL_USER.STATUS, user.status.name)
            })
        }
        arrayOf(MockResult(result.size, result))
    }, POSTGRES))

}
