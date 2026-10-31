package uk.co.renbinden.vantage.audit

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.jooq.Tables.AUDIT_LOG
import uk.co.renbinden.vantage.jooq.tables.records.AuditLogRecord
import uk.co.renbinden.vantage.user.UserId
import java.time.Instant
import java.time.ZoneOffset.UTC

/**
 * Exercises [AuditRepository] against jOOQ's [MockConnection], recording the SQL and bindings each
 * call sends and answering with canned rows. The SQL is rendered in the PostgreSQL dialect, as in
 * production, but is never executed against a real database.
 */
class AuditRepositoryTest {

    private val statements = mutableListOf<Pair<String, List<Any?>>>()
    private var rows: List<AuditLogRecord> = emptyList()
    private var count: Int? = null

    private val repository = AuditRepository(DSL.using(MockConnection { context ->
        statements.add(context.sql() to context.bindings().toList())
        if (context.sql().contains("count(*)")) {
            val result = DSL.using(POSTGRES).newResult(DSL.count())
            count?.let { result.add(DSL.using(POSTGRES).newRecord(DSL.count()).values(it)) }
            arrayOf(MockResult(result.size, result))
        } else {
            val result = DSL.using(POSTGRES).newResult(AUDIT_LOG)
            result.addAll(rows)
            arrayOf(MockResult(result.size, result))
        }
    }, POSTGRES))

    private fun record(userId: String, description: String, time: Instant): AuditLogRecord =
        DSL.using(POSTGRES).newRecord(AUDIT_LOG).apply {
            set(AUDIT_LOG.USER_ID, userId)
            set(AUDIT_LOG.DESCRIPTION, description)
            set(AUDIT_LOG.TIME, time.atOffset(UTC))
        }

    @Test
    fun `getAuditRecords orders by time newest first`() {
        repository.getAuditRecords()

        val (sql, _) = statements.single()
        assertTrue(sql.startsWith("select"), sql)
        assertTrue(sql.contains("order by \"audit_log\".\"time\" desc"), sql)
    }

    @Test
    fun `getAuditRecords binds an offset of 0 and a limit of 50 by default`() {
        repository.getAuditRecords()

        val (sql, bindings) = statements.single()
        assertTrue(sql.contains("offset ? rows fetch next ? rows only"), sql)
        assertEquals(listOf(0, 50), bindings.map { (it as Number).toInt() })
    }

    @Test
    fun `getAuditRecords binds the requested offset and amount`() {
        repository.getAuditRecords(amount = 7, offset = 3)

        assertEquals(listOf(3, 7), statements.single().second.map { (it as Number).toInt() })
    }

    @Test
    fun `getAuditRecords maps each returned row to an audit item in the order returned`() {
        val first = Instant.parse("2026-10-30T12:00:00Z")
        val second = Instant.parse("2026-10-29T08:30:00Z")
        rows = listOf(record("user-a", "Started server", first), record("user-b", "Created user bob", second))

        val items = repository.getAuditRecords()

        assertEquals(
            listOf(
                AuditItem(UserId("user-a"), "Started server", first),
                AuditItem(UserId("user-b"), "Created user bob", second)
            ),
            items
        )
    }

    @Test
    fun `getAuditRecords returns an empty list when there are no rows`() {
        assertEquals(emptyList<AuditItem>(), repository.getAuditRecords())
    }

    @Test
    fun `getCount returns the counted value`() {
        count = 42

        assertEquals(42, repository.getCount())
        assertTrue(statements.single().first.contains("from \"audit_log\""), statements.single().first)
    }

    @Test
    fun `getCount returns 0 when the count query returns no row`() {
        count = null

        assertEquals(0, repository.getCount())
    }

    @Test
    fun `insert binds the user id, description, and time at UTC and asks for the inserted row back`() {
        val time = Instant.parse("2026-10-30T09:15:30Z")
        rows = listOf(record("user-a", "Stopped server", time))

        repository.insert(AuditItem(UserId("user-a"), "Stopped server", time))

        val (sql, bindings) = statements.single()
        assertTrue(sql.startsWith("insert into \"audit_log\""), sql)
        assertTrue(sql.contains(") returning "), sql)
        assertEquals("user-a", bindings[0])
        assertEquals("Stopped server", bindings[1])
        // The PostgreSQL dialect binds timestamptz values as strings.
        assertEquals(time.atOffset(UTC), AUDIT_LOG.TIME.dataType.convert(bindings[2]))
        assertEquals(3, bindings.size)
    }

    @Test
    fun `insert returns the row the database returned rather than the item passed in`() {
        val returnedTime = Instant.parse("2026-10-30T09:15:31Z")
        rows = listOf(record("user-db", "Description from database", returnedTime))

        val inserted = repository.insert(AuditItem(UserId("user-a"), "Stopped server", Instant.parse("2026-10-30T09:15:30Z")))

        assertEquals(AuditItem(UserId("user-db"), "Description from database", returnedTime), inserted)
    }

    @Test
    fun `insert throws when the database returns no row`() {
        rows = emptyList()

        assertThrows<IllegalArgumentException> {
            repository.insert(AuditItem(UserId("user-a"), "Stopped server", Instant.parse("2026-10-30T09:15:30Z")))
        }
    }
}
