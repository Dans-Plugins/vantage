package uk.co.renbinden.vantage.audit

import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockResult
import uk.co.renbinden.vantage.jooq.Tables.AUDIT_LOG
import uk.co.renbinden.vantage.user.UserId

/**
 * An [AuditRepository] backed by jOOQ's [MockConnection] instead of PostgreSQL.
 *
 * Supports `insert(AuditItem)` only: each insert's bindings — user id, description, and time, in
 * that order — are appended to [items] and echoed back as the returned row.
 */
class InMemoryAuditRepository {

    val items = mutableListOf<AuditItem>()

    val repository = AuditRepository(DSL.using(MockConnection { context ->
        val (userId, description, time) = context.bindings()
        val record = DSL.using(POSTGRES).newRecord(AUDIT_LOG).apply {
            set(AUDIT_LOG.USER_ID, userId as String)
            set(AUDIT_LOG.DESCRIPTION, description as String)
            // The PostgreSQL dialect binds timestamptz values as strings.
            set(AUDIT_LOG.TIME, AUDIT_LOG.TIME.dataType.convert(time))
        }
        items.add(AuditItem(UserId(record.userId), record.description, record.time.toInstant()))
        val result = DSL.using(POSTGRES).newResult(AUDIT_LOG).apply { add(record) }
        arrayOf(MockResult(1, result))
    }, POSTGRES))

}
