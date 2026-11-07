package uk.co.renbinden.vantage.endpoint.api.v2.user

import io.jsonwebtoken.JwtException
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import uk.co.renbinden.vantage.user.UserStatus.ACTIVE
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE
import java.sql.SQLException

class UserHandlerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() = PrivateKeyFile.beforeAll()

        @JvmStatic
        @AfterAll
        fun afterAll() = PrivateKeyFile.afterAll()
    }

    private val alice = User("alice", "correct horse battery staple")
    private val bob = User("bob", "hunter2")
    private lateinit var users: InMemoryUserRepository
    private lateinit var audit: InMemoryAuditRepository
    private lateinit var token: String
    private lateinit var app: HttpHandler

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        users.users.add(alice)
        users.users.add(bob)
        audit = InMemoryAuditRepository()
        val authenticator = Authenticator(users.repository)
        token = authenticator.authenticate("alice", "correct horse battery staple")!!
        val handler = UserHandler(authenticator, users.repository, audit.repository, "https://panel.example")
        // Mirrors the /user routes in Vantage.kt, without the Authenticated filter.
        app = routes(
            "/api/v2/user" bind routes(
                "/{id}" bind GET to handler::get,
                "/" bind POST to handler::post,
                "/{id}" bind PATCH to handler::patch
            )
        )
    }

    private fun Request.authorized() = header("Authorization", "Bearer $token")

    private fun create(username: String, password: String) =
        Request(POST, "/api/v2/user/").with(UserCreateRequest.lens of UserCreateRequest(username, password))

    private fun update(user: User, request: UserUpdateRequest) =
        Request(PATCH, "/api/v2/user/${user.id.value}").with(UserUpdateRequest.lens of request)

    private fun storedUser(username: String) = users.users.single { it.username == username }

    @Test
    fun `get responds with the user's id, version, username, and status`() {
        val response = app(Request(GET, "/api/v2/user/${bob.id.value}"))

        assertEquals(OK, response.status)
        assertEquals(UserResponse(bob.id.value, 0, "bob", ACTIVE), UserResponse.lens(response))
    }

    @Test
    fun `get leaves the password hash and salt out of the response`() {
        val response = app(Request(GET, "/api/v2/user/${bob.id.value}"))

        assertFalse(response.bodyString().contains("password"), response.bodyString())
    }

    @Test
    fun `get responds 404 for an unknown id`() {
        val response = app(Request(GET, "/api/v2/user/${User("carol", "pw").id.value}"))

        assertEquals(NOT_FOUND, response.status)
    }

    @Test
    fun `post creates an active user at version 1 and responds 201 with its location`() {
        val response = app(create("carol", "s3cret").authorized())

        assertEquals(CREATED, response.status)
        val carol = storedUser("carol")
        assertTrue(carol.checkPassword("s3cret"))
        assertEquals(UserResponse(carol.id.value, 1, "carol", ACTIVE), UserResponse.lens(response))
        assertEquals("https://panel.example/api/v2/user/${carol.id.value}", response.header("Location"))
    }

    @Test
    fun `post records the creation in the audit log against the acting user`() {
        app(create("carol", "s3cret").authorized())

        val item = audit.items.single()
        assertEquals(alice.id, item.userId)
        assertEquals("Created user carol (${storedUser("carol").id.value})", item.description)
    }

    @Test
    fun `post without a token responds 401, creates nothing, and writes no audit entry`() {
        val response = app(create("carol", "s3cret"))

        assertEquals(UNAUTHORIZED, response.status)
        assertEquals(listOf(alice, bob), users.users)
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `post with a token that fails verification creates nothing`() {
        assertThrows<JwtException> {
            app(create("carol", "s3cret").header("Authorization", "Bearer not-a-token"))
        }

        assertEquals(listOf(alice, bob), users.users)
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `post throws instead of responding when the write fails, and writes no audit entry`() {
        // A taken username fails this way: the upsert's conflict target is the id, not the username.
        users.upsertFailure = SQLException("duplicate key value violates unique constraint")

        assertThrows<DataAccessException> { app(create("bob", "s3cret").authorized()) }

        assertEquals(listOf(alice, bob), users.users)
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `post throws a LensFailure for a body that is not JSON instead of responding`() {
        assertThrows<LensFailure> {
            app(Request(POST, "/api/v2/user/").body("not json").authorized())
        }

        assertEquals(listOf(alice, bob), users.users)
    }

    @Test
    fun `patch throws a LensFailure for a body that is not JSON instead of responding`() {
        assertThrows<LensFailure> {
            app(Request(PATCH, "/api/v2/user/${bob.id.value}").body("not json").authorized())
        }

        assertEquals(bob, storedUser("bob"))
    }

    @Test
    fun `patch changes the username and status, keeps the password, and bumps the version`() {
        val response = app(update(bob, UserUpdateRequest(0, "robert", null, INACTIVE)).authorized())

        assertEquals(OK, response.status)
        assertEquals(UserResponse(bob.id.value, 1, "robert", INACTIVE), UserResponse.lens(response))
        val robert = storedUser("robert")
        assertTrue(robert.checkPassword("hunter2"))
    }

    @Test
    fun `patch with only a password keeps the username and status`() {
        val response = app(update(bob, UserUpdateRequest(0, null, "hunter3", null)).authorized())

        assertEquals(UserResponse(bob.id.value, 1, "bob", ACTIVE), UserResponse.lens(response))
        val stored = storedUser("bob")
        assertTrue(stored.checkPassword("hunter3"))
        assertFalse(stored.checkPassword("hunter2"))
    }

    @Test
    fun `patch records the update in the audit log against the acting user`() {
        app(update(bob, UserUpdateRequest(0, "robert", null, null)).authorized())

        val item = audit.items.single()
        assertEquals(alice.id, item.userId)
        assertEquals("Updated user robert (${bob.id.value})", item.description)
    }

    @Test
    fun `patch with a stale version responds 409 and changes nothing`() {
        val response = app(update(bob, UserUpdateRequest(5, "robert", null, null)).authorized())

        assertEquals(CONFLICT, response.status)
        assertEquals(bob, storedUser("bob"))
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `patch responds 500 when the update fails for any other reason and writes no audit entry`() {
        users.upsertFailure = SQLException("connection lost")

        val response = app(update(bob, UserUpdateRequest(0, "robert", null, null)).authorized())

        assertEquals(INTERNAL_SERVER_ERROR, response.status)
        assertEquals(bob, storedUser("bob"))
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `patch responds 404 for an unknown id`() {
        val carol = User("carol", "pw")

        val response = app(update(carol, UserUpdateRequest(0, "caroline", null, null)).authorized())

        assertEquals(NOT_FOUND, response.status)
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `patch without a token responds 401, changes nothing, and writes no audit entry`() {
        val response = app(update(bob, UserUpdateRequest(0, "robert", null, null)))

        assertEquals(UNAUTHORIZED, response.status)
        assertEquals(bob, storedUser("bob"))
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `patch with a token that fails verification changes nothing`() {
        assertThrows<JwtException> {
            app(update(bob, UserUpdateRequest(0, "robert", null, null)).header("Authorization", "Bearer not-a-token"))
        }

        assertEquals(bob, storedUser("bob"))
        assertTrue(audit.items.isEmpty())
    }

}
