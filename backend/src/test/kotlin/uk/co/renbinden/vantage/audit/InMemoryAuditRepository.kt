package uk.co.renbinden.vantage.audit

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import uk.co.renbinden.vantage.jooq.Tables.AUDIT_LOG
import uk.co.renbinden.vantage.jooq.tables.records.AuditLogRecord
import uk.co.renbinden.vantage.user.UserId
import java.time.ZoneOffset.UTC

/**
 * An [AuditRepository] backed by jOOQ's [MockConnection] instead of PostgreSQL.
 *
 * Supports every [AuditRepository] query:
 * - `insert(AuditItem)`, by appending the insert's bindings — user id, description, and time, in
 *   that order — to [items] and echoing them back as the returned row;
 * - `getAuditRecords(amount, offset)`, by sorting [items] newest first and applying the offset and
 *   limit;
 * - `getCount()`, by counting [items].
 *
 * Replace the contents of [items] to change what later queries return.
 */
class InMemoryAuditRepository {

    val items = mutableListOf<AuditItem>()

    val repository = AuditRepository(DSL.using(MockConnection { context ->
        val sql = context.sql()
        val bindings = context.bindings()
        when {
            sql.startsWith("insert") -> {
                val (userId, description, time) = bindings
                val record = DSL.using(POSTGRES).newRecord(AUDIT_LOG).apply {
                    set(AUDIT_LOG.USER_ID, userId as String)
                    set(AUDIT_LOG.DESCRIPTION, description as String)
                    // The PostgreSQL dialect binds timestamptz values as strings.
                    set(AUDIT_LOG.TIME, AUDIT_LOG.TIME.dataType.convert(time))
                }
                items.add(AuditItem(UserId(record.userId), record.description, record.time.toInstant()))
                arrayOf(auditResult(listOf(record)))
            }
            sql.contains("count(*)") -> {
                val result = DSL.using(POSTGRES).newResult(DSL.count())
                result.add(DSL.using(POSTGRES).newRecord(DSL.count()).values(items.size))
                arrayOf(MockResult(1, result))
            }
            // The PostgreSQL dialect renders offset/limit as "offset ? rows fetch next ? rows only".
            else -> {
                val (offset, limit) = bindings.map { (it as Number).toInt() }
                arrayOf(auditResult(items.sortedByDescending { it.time }.drop(offset).take(limit).map(::toRecord)))
            }
        }
    }, POSTGRES))

    private fun auditResult(records: List<AuditLogRecord>): MockResult {
        val result = DSL.using(POSTGRES).newResult(AUDIT_LOG)
        result.addAll(records)
        return MockResult(result.size, result)
    }

    private fun toRecord(item: AuditItem): AuditLogRecord = DSL.using(POSTGRES).newRecord(AUDIT_LOG).apply {
        set(AUDIT_LOG.USER_ID, item.userId.value)
        set(AUDIT_LOG.DESCRIPTION, item.description)
        set(AUDIT_LOG.TIME, item.time.atOffset(UTC))
    }

}
