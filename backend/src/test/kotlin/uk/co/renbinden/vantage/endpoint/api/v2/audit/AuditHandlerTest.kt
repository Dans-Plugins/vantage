package uk.co.renbinden.vantage.endpoint.api.v2.audit

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.audit.AuditItem
import uk.co.renbinden.vantage.audit.InMemoryAuditRepository
import uk.co.renbinden.vantage.auth.InMemoryUserRepository
import uk.co.renbinden.vantage.endpoint.api.v2.PaginationResponse
import uk.co.renbinden.vantage.endpoint.api.v2.user.UserResponse
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserId
import uk.co.renbinden.vantage.user.UserStatus.ACTIVE
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE
import java.time.Instant

class AuditHandlerTest {

    private lateinit var users: InMemoryUserRepository
    private lateinit var audit: InMemoryAuditRepository
    private lateinit var app: HttpHandler

    private val start = Instant.parse("2026-01-02T03:04:05Z")

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        audit = InMemoryAuditRepository()
        // Mirrors the /audit/{page} route in Vantage.kt, without the Authenticated filter.
        app = routes("/api/v2/audit/{page}" bind GET to AuditHandler(audit.repository, users.repository)::get)
    }

    // Skips PBKDF2 hashing, which the handler never looks at.
    private fun user(username: String) = User(UserId.generate(), 0, username, ByteArray(0), ByteArray(0), ACTIVE)

    private fun items(user: User, count: Int) =
        (0 until count).map { AuditItem(user.id, "Action %02d".format(it), start.plusSeconds(it.toLong())) }

    @Test
    fun `responds with audit items newest first, each with its user, and the total count`() {
        val alice = user("alice").copy(version = 2)
        val bob = user("bob").copy(status = INACTIVE)
        users.users.addAll(listOf(alice, bob))
        audit.items.addAll(
            listOf(
                AuditItem(alice.id, "Logged in", start),
                AuditItem(bob.id, "Created user carol (1234)", start.plusSeconds(60))
            )
        )

        val response = app(Request(GET, "/api/v2/audit/0"))

        assertEquals(OK, response.status)
        assertEquals(
            AuditResponse(
                listOf(
                    AuditItemResponse(
                        UserResponse(bob.id.value, 0, "bob", INACTIVE),
                        "Created user carol (1234)",
                        start.plusSeconds(60)
                    ),
                    AuditItemResponse(UserResponse(alice.id.value, 2, "alice", ACTIVE), "Logged in", start)
                ),
                PaginationResponse(2)
            ),
            AuditResponse.lens(response)
        )
    }

    @Test
    fun `serializes item times as ISO-8601 strings`() {
        val alice = user("alice")
        users.users.add(alice)
        audit.items.add(AuditItem(alice.id, "Logged in", start))

        val response = app(Request(GET, "/api/v2/audit/0"))

        assertTrue(response.bodyString().contains("\"time\":\"2026-01-02T03:04:05Z\""), response.bodyString())
    }

    @Test
    fun `leaves password hashes and salts out of each item's user`() {
        val alice = User("alice", "correct horse battery staple")
        users.users.add(alice)
        audit.items.add(AuditItem(alice.id, "Logged in", start))

        val response = app(Request(GET, "/api/v2/audit/0"))

        assertFalse(response.bodyString().contains("password"), response.bodyString())
    }

    @Test
    fun `responds with an empty list and a zero count when the audit log is empty`() {
        val response = app(Request(GET, "/api/v2/audit/0"))

        assertEquals(AuditResponse(emptyList(), PaginationResponse(0)), AuditResponse.lens(response))
    }

    @Test
    fun `returns 50 items per page while counting all of them`() {
        val alice = user("alice")
        users.users.add(alice)
        audit.items.addAll(items(alice, 51))

        val firstPage = AuditResponse.lens(app(Request(GET, "/api/v2/audit/0")))
        val secondPage = AuditResponse.lens(app(Request(GET, "/api/v2/audit/1")))

        assertEquals(50, firstPage.items.size)
        assertEquals("Action 50", firstPage.items.first().description)
        assertEquals("Action 01", firstPage.items.last().description)
        assertEquals(PaginationResponse(51), firstPage.pagination)
        assertEquals(listOf("Action 00"), secondPage.items.map { it.description })
        assertEquals(PaginationResponse(51), secondPage.pagination)
    }

    @Test
    fun `treats a page that is not a number as the first page`() {
        val alice = user("alice")
        users.users.add(alice)
        audit.items.addAll(items(alice, 51))

        val body = AuditResponse.lens(app(Request(GET, "/api/v2/audit/latest")))

        assertEquals("Action 50", body.items.first().description)
    }

    @Test
    fun `throws when an audit item's user no longer exists`() {
        audit.items.add(AuditItem(UserId.generate(), "Logged in", start))

        assertThrows<IllegalArgumentException> { app(Request(GET, "/api/v2/audit/0")) }
    }

}
