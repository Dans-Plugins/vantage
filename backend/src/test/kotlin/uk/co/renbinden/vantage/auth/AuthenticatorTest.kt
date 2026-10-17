package uk.co.renbinden.vantage.auth

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm.RS256
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE
import java.time.Duration
import java.time.Instant

class AuthenticatorTest {

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
    fun `constructing an authenticator writes a PEM private key to the working directory when none exists`() {
        assumeTrue(PrivateKeyFile.createdByTests, "a private.pem already existed in the working directory")

        val lines = PrivateKeyFile.file.readLines()

        assertEquals("-----BEGIN PRIVATE KEY-----", lines.first())
        assertEquals("-----END PRIVATE KEY-----", lines.last())
    }

    @Test
    fun `authenticators constructed from the same private key accept each other's tokens`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!

        val otherAuthenticator = Authenticator(users.repository)

        assertEquals(alice.id.value, otherAuthenticator.verify(token).body.subject)
    }

    @Test
    fun `authenticate returns a token whose subject is the user's id`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")

        assertNotNull(token)
        assertEquals(alice.id.value, authenticator.verify(token!!).body.subject)
    }

    @Test
    fun `authenticate issues tokens that expire twenty-four hours after issue`() {
        val before = Instant.now()
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!
        val after = Instant.now()

        val expiration = authenticator.verify(token).body.expiration.toInstant()

        // JWT expiry is stored in whole seconds, so allow for the truncation.
        assertTrue(!expiration.isBefore(before.plus(Duration.ofHours(24)).minusSeconds(1)))
        assertTrue(!expiration.isAfter(after.plus(Duration.ofHours(24))))
    }

    @Test
    fun `authenticate returns null for an unknown username`() {
        assertNull(authenticator.authenticate("bob", "correct horse battery staple"))
    }

    @Test
    fun `authenticate returns null for the wrong password`() {
        assertNull(authenticator.authenticate("alice", "Tr0ub4dor&3"))
    }

    @Test
    fun `authenticate returns null for an inactive user with the right password`() {
        users.users.clear()
        users.users.add(alice.copy(status = INACTIVE))

        assertNull(authenticator.authenticate("alice", "correct horse battery staple"))
    }

    @Test
    fun `verify rejects a token whose signature has been tampered with`() {
        val token = authenticator.authenticate("alice", "correct horse battery staple")!!
        val (header, payload, signature) = token.split(".")
        val tamperedSignature = (if (signature.first() == 'A') "B" else "A") + signature.drop(1)

        assertThrows<JwtException> { authenticator.verify("$header.$payload.$tamperedSignature") }
    }

    @Test
    fun `verify rejects a token signed with a different key`() {
        val token = Jwts.builder()
            .setSubject(alice.id.value)
            .signWith(Keys.keyPairFor(RS256).private)
            .compact()

        assertThrows<JwtException> { authenticator.verify(token) }
    }

    @Test
    fun `verify rejects an unsigned token`() {
        val token = Jwts.builder()
            .setSubject(alice.id.value)
            .compact()

        assertThrows<JwtException> { authenticator.verify(token) }
    }

}
