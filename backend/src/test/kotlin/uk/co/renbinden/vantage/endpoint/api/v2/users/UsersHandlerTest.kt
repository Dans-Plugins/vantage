package uk.co.renbinden.vantage.endpoint.api.v2.users

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.auth.InMemoryUserRepository
import uk.co.renbinden.vantage.endpoint.api.v2.PaginationResponse
import uk.co.renbinden.vantage.endpoint.api.v2.user.UserResponse
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserId
import uk.co.renbinden.vantage.user.UserStatus.ACTIVE
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE

class UsersHandlerTest {

    private lateinit var users: InMemoryUserRepository
    private lateinit var handler: UsersHandler
    private lateinit var app: HttpHandler

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        handler = UsersHandler(users.repository)
        // Mirrors the /users route in Vantage.kt, without the Authenticated filter.
        app = routes("/api/v2/users" bind GET to handler::get)
    }

    // Skips PBKDF2 hashing, which the handler never looks at.
    private fun user(username: String) = User(UserId.generate(), 0, username, ByteArray(0), ByteArray(0), ACTIVE)

    @Test
    fun `responds with every user ordered by username and the total count`() {
        val carol = user("carol")
        val alice = user("alice").copy(version = 3)
        val bob = user("bob").copy(status = INACTIVE)
        users.users.addAll(listOf(carol, alice, bob))

        val response = app(Request(GET, "/api/v2/users"))

        assertEquals(OK, response.status)
        assertEquals(
            UsersResponse(
                listOf(
                    UserResponse(alice.id.value, 3, "alice", ACTIVE),
                    UserResponse(bob.id.value, 0, "bob", INACTIVE),
                    UserResponse(carol.id.value, 0, "carol", ACTIVE)
                ),
                PaginationResponse(3)
            ),
            UsersResponse.lens(response)
        )
    }

    @Test
    fun `responds with an empty list and a zero count when there are no users`() {
        val response = app(Request(GET, "/api/v2/users"))

        assertEquals(UsersResponse(emptyList(), PaginationResponse(0)), UsersResponse.lens(response))
    }

    @Test
    fun `leaves password hashes and salts out of the response`() {
        users.users.add(User("alice", "correct horse battery staple"))

        val response = app(Request(GET, "/api/v2/users"))

        assertFalse(response.bodyString().contains("password"), response.bodyString())
    }

    @Test
    fun `returns only the first 50 users while counting all of them`() {
        users.users.addAll((0 until 51).map { user("user%02d".format(it)) })

        val body = UsersResponse.lens(app(Request(GET, "/api/v2/users")))

        assertEquals(50, body.users.size)
        assertEquals("user49", body.users.last().username)
        assertEquals(PaginationResponse(51), body.pagination)
    }

    @Test
    fun `always returns the first page, because the users route binds no page path variable`() {
        users.users.addAll((0 until 51).map { user("user%02d".format(it)) })

        val body = UsersResponse.lens(app(Request(GET, "/api/v2/users?page=1")))

        assertEquals("user00", body.users.first().username)
    }

    @Test
    fun `skips 50 users per page when called with a page path variable`() {
        users.users.addAll((0 until 51).map { user("user%02d".format(it)) })
        val paged = routes("/api/v2/users/{page}" bind GET to handler::get)

        val body = UsersResponse.lens(paged(Request(GET, "/api/v2/users/1")))

        assertEquals(listOf("user50"), body.users.map { it.username })
        assertEquals(PaginationResponse(51), body.pagination)
    }

}
