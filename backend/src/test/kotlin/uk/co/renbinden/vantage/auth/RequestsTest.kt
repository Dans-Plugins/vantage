package uk.co.renbinden.vantage.auth

import io.jsonwebtoken.JwtException
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.user.User

class RequestsTest {

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

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        users.users.add(alice)
        authenticator = Authenticator(users.repository)
    }

    @Test
    fun `userId returns the subject of a valid bearer token`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!

        val userId = Request(GET, "/").header("Authorization", "Bearer $token").userId(authenticator)

        assertEquals(alice.id, userId)
    }

    @Test
    fun `userId does not look the user up, so it still resolves after the user is removed`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!
        users.users.clear()

        val userId = Request(GET, "/").header("Authorization", "Bearer $token").userId(authenticator)

        assertEquals(alice.id, userId)
    }

    @Test
    fun `userId returns null with no Authorization header`() {
        assertNull(Request(GET, "/").userId(authenticator))
    }

    @Test
    fun `userId returns null for a token sent without the Bearer scheme`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!

        assertNull(Request(GET, "/").header("Authorization", token).userId(authenticator))
    }

    @Test
    fun `userId throws rather than returning null for a malformed bearer token`() {
        val request = Request(GET, "/").header("Authorization", "Bearer not-a-jwt")

        assertThrows<JwtException> { request.userId(authenticator) }
    }

}
