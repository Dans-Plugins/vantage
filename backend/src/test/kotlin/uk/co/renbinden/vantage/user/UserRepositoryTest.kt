package uk.co.renbinden.vantage.user

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.failure.OptimisticLockingFailureException
import uk.co.renbinden.vantage.jooq.Tables.PANEL_USER
import uk.co.renbinden.vantage.jooq.tables.records.PanelUserRecord
import uk.co.renbinden.vantage.user.UserStatus.ACTIVE
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE

/**
 * Exercises [UserRepository] against jOOQ's [MockConnection], recording the SQL and bindings each
 * call sends and answering with canned rows. The SQL is rendered in the PostgreSQL dialect, as in
 * production, but is never executed against a real database, so nothing here proves that
 * PostgreSQL applies the `on conflict ... where` clause the way these tests read it.
 */
class UserRepositoryTest {

    private val statements = mutableListOf<Pair<String, List<Any?>>>()
    private var rows: List<PanelUserRecord> = emptyList()
    private var count: Int? = null

    private val repository = UserRepository(DSL.using(MockConnection { context ->
        statements.add(context.sql() to context.bindings().toList())
        if (context.sql().contains("count(*)")) {
            val result = DSL.using(POSTGRES).newResult(DSL.count())
            count?.let { result.add(DSL.using(POSTGRES).newRecord(DSL.count()).values(it)) }
            arrayOf(MockResult(result.size, result))
        } else {
            val result = DSL.using(POSTGRES).newResult(PANEL_USER)
            result.addAll(rows)
            arrayOf(MockResult(result.size, result))
        }
    }, POSTGRES))

    private val hash = byteArrayOf(1, 2, 3)
    private val salt = byteArrayOf(4, 5, 6)

    private fun user(id: String = "user-a", version: Int = 3, username: String = "alice", status: UserStatus = ACTIVE) =
        User(UserId(id), version, username, hash, salt, status)

    private fun record(user: User): PanelUserRecord = record(user, user.status.name)

    private fun record(user: User, status: String?): PanelUserRecord =
        DSL.using(POSTGRES).newRecord(PANEL_USER).apply {
            set(PANEL_USER.ID, user.id.value)
            set(PANEL_USER.VERSION, user.version)
            set(PANEL_USER.USERNAME, user.username)
            set(PANEL_USER.PASSWORD_HASH, user.passwordHash)
            set(PANEL_USER.PASSWORD_SALT, user.passwordSalt)
            set(PANEL_USER.STATUS, status)
        }

    @Test
    fun `getUser by id filters on the id column and maps the row`() {
        val stored = user(status = INACTIVE)
        rows = listOf(record(stored))

        val found = repository.getUser(UserId("user-a"))

        assertEquals(stored, found)
        val (sql, bindings) = statements.single()
        assertTrue(sql.contains("where \"panel_user\".\"id\" = ?"), sql)
        assertEquals(listOf<Any?>("user-a"), bindings)
    }

    @Test
    fun `getUser by id returns null when no row matches`() {
        assertNull(repository.getUser(UserId("missing")))
    }

    @Test
    fun `getUser by username filters on the username column and maps the row`() {
        val stored = user()
        rows = listOf(record(stored))

        val found = repository.getUser("alice")

        assertEquals(stored, found)
        val (sql, bindings) = statements.single()
        assertTrue(sql.contains("where \"panel_user\".\"username\" = ?"), sql)
        assertEquals(listOf<Any?>("alice"), bindings)
    }

    @Test
    fun `getUser by username returns null when no row matches`() {
        assertNull(repository.getUser("missing"))
    }

    @Test
    fun `reading a row whose status is not a UserStatus name throws`() {
        rows = listOf(record(user(), "BANNED"))

        assertThrows<IllegalArgumentException> { repository.getUser("alice") }
    }

    // V1__Initial_version.sql declares panel_user.status without "not null".
    @Test
    fun `reading a row whose status is null throws`() {
        rows = listOf(record(user(), null))

        assertThrows<NullPointerException> { repository.getUser("alice") }
    }

    @Test
    fun `getUsers orders by username`() {
        repository.getUsers()

        val (sql, _) = statements.single()
        assertTrue(sql.startsWith("select"), sql)
        assertTrue(sql.contains("order by \"panel_user\".\"username\""), sql)
    }

    @Test
    fun `getUsers binds an offset of 0 and a limit of 50 by default`() {
        repository.getUsers()

        val (sql, bindings) = statements.single()
        assertTrue(sql.contains("offset ? rows fetch next ? rows only"), sql)
        assertEquals(listOf(0, 50), bindings.map { (it as Number).toInt() })
    }

    @Test
    fun `getUsers binds the requested offset and amount`() {
        repository.getUsers(amount = 7, offset = 3)

        assertEquals(listOf(3, 7), statements.single().second.map { (it as Number).toInt() })
    }

    @Test
    fun `getUsers maps each returned row to a user in the order returned`() {
        val first = user(id = "user-b", username = "bob")
        val second = user(id = "user-a", username = "alice", status = INACTIVE)
        rows = listOf(record(first), record(second))

        assertEquals(listOf(first, second), repository.getUsers())
    }

    @Test
    fun `getUsers returns an empty list when there are no rows`() {
        assertEquals(emptyList<User>(), repository.getUsers())
    }

    @Test
    fun `getUserCount returns the counted value`() {
        count = 42

        assertEquals(42, repository.getUserCount())
        assertTrue(statements.single().first.contains("from \"panel_user\""), statements.single().first)
    }

    // Only reachable through the mock: "select count(*)" always returns a row on a real database.
    @Test
    fun `getUserCount returns 0 when the count query returns no row`() {
        count = null

        assertEquals(0, repository.getUserCount())
    }

    @Test
    fun `upsert inserts with on conflict on the id, guarded by the id and the expected version`() {
        rows = listOf(record(user()))

        repository.upsert(user())

        val (sql, _) = statements.single()
        assertTrue(sql.startsWith("insert into \"panel_user\""), sql)
        assertTrue(sql.contains("on conflict (\"id\") do update set"), sql)
        assertTrue(sql.contains("where (\"panel_user\".\"id\" = ? and \"panel_user\".\"version\" = ?)"), sql)
        assertTrue(sql.contains(" returning "), sql)
    }

    @Test
    fun `upsert binds the insert at version 1 whatever version the user carries`() {
        val user = user(version = 9)
        rows = listOf(record(user))

        repository.upsert(user)

        val bindings = statements.single().second
        assertEquals("user-a", bindings[0])
        assertEquals(1, (bindings[1] as Number).toInt())
        assertEquals("alice", bindings[2])
        assertSame(hash, bindings[3])
        assertSame(salt, bindings[4])
        assertEquals("ACTIVE", bindings[5])
    }

    @Test
    fun `upsert binds the update at the next version and guards it with the current version`() {
        val user = user(version = 9, status = INACTIVE)
        rows = listOf(record(user))

        repository.upsert(user)

        val bindings = statements.single().second
        assertEquals("alice", bindings[6])
        assertSame(hash, bindings[7])
        assertSame(salt, bindings[8])
        assertEquals("INACTIVE", bindings[9])
        assertEquals(10, (bindings[10] as Number).toInt())
        assertEquals("user-a", bindings[11])
        assertEquals(9, (bindings[12] as Number).toInt())
        assertEquals(13, bindings.size)
    }

    @Test
    fun `upsert returns the row the database returned rather than the user passed in`() {
        val returned = user(id = "user-db", version = 4, username = "from-database", status = INACTIVE)
        rows = listOf(record(returned))

        assertEquals(returned, repository.upsert(user()))
    }

    @Test
    fun `upsert throws naming the version when the database returns no row`() {
        rows = emptyList()

        val exception = assertThrows<OptimisticLockingFailureException> { repository.upsert(user(version = 9)) }

        assertEquals("Invalid version: 9", exception.message)
    }
}
