package uk.co.renbinden.vantage.endpoint.api.v2.login

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.audit.InMemoryAuditRepository
import uk.co.renbinden.vantage.auth.Authenticator
import uk.co.renbinden.vantage.auth.InMemoryUserRepository
import uk.co.renbinden.vantage.auth.PrivateKeyFile
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE
import java.time.Instant

class LoginHandlerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() = PrivateKeyFile.beforeAll()

        @JvmStatic
        @AfterAll
        fun afterAll() = PrivateKeyFile.afterAll()
    }

    private val alice = User("alice", "correct horse battery staple")
    private lateinit var users: InMemoryUserRepository
    private lateinit var audit: InMemoryAuditRepository
    private lateinit var authenticator: Authenticator
    private lateinit var handler: LoginHandler

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        users.users.add(alice)
        audit = InMemoryAuditRepository()
        authenticator = Authenticator(users.repository)
        handler = LoginHandler(authenticator, users.repository, audit.repository)
    }

    private fun login(username: String, password: String) =
        handler.post(Request(POST, "/api/v2/login").with(LoginRequest.lens of LoginRequest(username, password)))

    @Test
    fun `responds with a token for the user's id, the user's id, and the username`() {
        val response = login("alice", "correct horse battery staple")

        assertEquals(OK, response.status)
        val body = LoginResponse.lens(response)
        assertEquals(alice.id, body.userId)
        assertEquals("alice", body.username)
        assertEquals(alice.id.value, authenticator.verify(body.token).body.subject)
    }

    @Test
    fun `serializes the user id as a plain string`() {
        val response = login("alice", "correct horse battery staple")

        assertTrue(response.bodyString().contains("\"userId\":\"${alice.id.value}\""), response.bodyString())
    }

    @Test
    fun `records a successful login in the audit log as Logged in`() {
        val before = Instant.now()
        login("alice", "correct horse battery staple")
        val after = Instant.now()

        val item = audit.items.single()
        assertEquals(alice.id, item.userId)
        assertEquals("Logged in", item.description)
        assertTrue(!item.time.isBefore(before) && !item.time.isAfter(after))
    }

    @Test
    fun `rejects the wrong password with an empty 401 and no audit entry`() {
        val response = login("alice", "Tr0ub4dor&3")

        assertEquals(UNAUTHORIZED, response.status)
        assertEquals("", response.bodyString())
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `rejects an unknown username with an empty 401 and no audit entry`() {
        val response = login("bob", "correct horse battery staple")

        assertEquals(UNAUTHORIZED, response.status)
        assertEquals("", response.bodyString())
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `rejects an inactive user with the right password and no audit entry`() {
        users.users.clear()
        users.users.add(alice.copy(status = INACTIVE))

        val response = login("alice", "correct horse battery staple")

        assertEquals(UNAUTHORIZED, response.status)
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `throws a LensFailure for a body that is not JSON instead of responding`() {
        assertThrows<LensFailure> { handler.post(Request(POST, "/api/v2/login").body("username=alice")) }
    }

}
