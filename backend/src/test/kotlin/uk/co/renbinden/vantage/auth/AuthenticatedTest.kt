package uk.co.renbinden.vantage.auth

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE

class AuthenticatedTest {

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
    private lateinit var authenticator: Authenticator
    private lateinit var token: String
    private var nextCalled = false
    private lateinit var app: (Request) -> Response

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        users.users.add(alice)
        authenticator = Authenticator(users.repository)
        token = authenticator.authenticate("alice", "correct horse battery staple")!!
        nextCalled = false
        // Mirrors how Vantage.kt wraps every route except /api/v2/login.
        app = Authenticated(authenticator, users.repository).then { _ ->
            nextCalled = true
            Response(OK).body("protected")
        }
    }

    @Test
    fun `passes a bearer token for an active user through to the wrapped handler`() {
        val response = app(Request(GET, "/").header("Authorization", "Bearer $token"))

        assertEquals(OK, response.status)
        assertEquals("protected", response.bodyString())
    }

    @Test
    fun `rejects a request with no Authorization header`() {
        val response = app(Request(GET, "/"))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `rejects a token sent without the Bearer scheme`() {
        val response = app(Request(GET, "/").header("Authorization", token))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `matches the Bearer scheme case-sensitively`() {
        val response = app(Request(GET, "/").header("Authorization", "bearer $token"))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `rejects a malformed token`() {
        val response = app(Request(GET, "/").header("Authorization", "Bearer not-a-jwt"))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `rejects a valid token for a user that no longer exists`() {
        users.users.clear()

        val response = app(Request(GET, "/").header("Authorization", "Bearer $token"))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `rejects a valid token for a user deactivated after the token was issued`() {
        users.users.clear()
        users.users.add(alice.copy(status = INACTIVE))

        val response = app(Request(GET, "/").header("Authorization", "Bearer $token"))

        assertEquals(UNAUTHORIZED, response.status)
        assertFalse(nextCalled)
    }

    @Test
    fun `responds with an empty body when rejecting`() {
        val response = app(Request(GET, "/"))

        assertEquals("", response.bodyString())
    }

}
