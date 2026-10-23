package uk.co.renbinden.vantage.websocket

import io.jsonwebtoken.Jwts
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.http4k.websocket.WsStatus.Companion.POLICY_VALIDATION
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.audit.InMemoryAuditRepository
import uk.co.renbinden.vantage.auth.Authenticator
import uk.co.renbinden.vantage.auth.InMemoryUserRepository
import uk.co.renbinden.vantage.auth.PrivateKeyFile
import uk.co.renbinden.vantage.config.MinecraftConfig
import uk.co.renbinden.vantage.minecraft.MinecraftServer
import uk.co.renbinden.vantage.user.User
import uk.co.renbinden.vantage.user.UserStatus.INACTIVE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*

class ConsoleHandlerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() = PrivateKeyFile.beforeAll()

        @JvmStatic
        @AfterAll
        fun afterAll() = PrivateKeyFile.afterAll()
    }

    /** Records what [ConsoleHandler] does to a socket and lets a test send it messages. */
    private class FakeWebsocket(override val upgradeRequest: Request) : Websocket {
        val sent = mutableListOf<WsMessage>()
        var closedWith: WsStatus? = null
        private val messageHandlers = mutableListOf<(WsMessage) -> Unit>()
        private val closeHandlers = mutableListOf<(WsStatus) -> Unit>()

        override fun send(message: WsMessage) {
            sent.add(message)
        }

        override fun close(status: WsStatus) {
            closedWith = status
        }

        override fun onError(fn: (Throwable) -> Unit) {}

        override fun onClose(fn: (WsStatus) -> Unit) {
            closeHandlers.add(fn)
        }

        override fun onMessage(fn: (WsMessage) -> Unit) {
            messageHandlers.add(fn)
        }

        fun receive(json: String) = messageHandlers.forEach { it(WsMessage(json)) }

        fun clientCloses() = closeHandlers.forEach { it(WsStatus.NORMAL) }
    }

    /** Stands in for the Minecraft process so console input can be read back without spawning one. */
    private class FakeProcess : Process() {
        val input = ByteArrayOutputStream()
        override fun getOutputStream(): OutputStream = input
        override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun waitFor() = 0
        override fun exitValue() = 0
        override fun destroy() {}
    }

    private val alice = User("alice", "correct horse battery staple")
    private lateinit var users: InMemoryUserRepository
    private lateinit var audit: InMemoryAuditRepository
    private lateinit var authenticator: Authenticator
    private lateinit var minecraftServer: MinecraftServer
    private lateinit var handler: ConsoleHandler
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        users.users.add(alice)
        audit = InMemoryAuditRepository()
        authenticator = Authenticator(users.repository)
        minecraftServer = MinecraftServer(MinecraftConfig())
        handler = ConsoleHandler(authenticator, users.repository, audit.repository, minecraftServer)
        token = authenticator.authenticate("alice", "correct horse battery staple")!!
    }

    private fun open(query: String = "?token=$token"): FakeWebsocket {
        val websocket = FakeWebsocket(Request(GET, "/ws/log$query"))
        handler.onSocketOpen(websocket)
        return websocket
    }

    private fun assertRejected(websocket: FakeWebsocket, description: String) {
        assertEquals(POLICY_VALIDATION.code, websocket.closedWith?.code)
        assertEquals(description, websocket.closedWith?.description)
        assertFalse(minecraftServer.consoleSockets.contains(websocket))
    }

    private fun signingKey() = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(
        Base64.getDecoder().decode(
            PrivateKeyFile.file.readText()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
        )
    ))

    private fun startFakeServer(): FakeProcess {
        val process = FakeProcess()
        minecraftServer.process = process
        minecraftServer.isRunning = true
        return process
    }

    @Test
    fun `registers a socket opened with a valid token as a console socket`() {
        val websocket = open()

        assertNull(websocket.closedWith)
        assertTrue(minecraftServer.consoleSockets.contains(websocket))
    }

    @Test
    fun `closes a socket opened without a token parameter`() {
        assertRejected(open(""), "token parameter must be specified")
    }

    @Test
    fun `closes a socket opened with an empty token`() {
        assertRejected(open("?token="), "empty token")
    }

    @Test
    fun `closes a socket opened with a malformed token`() {
        assertRejected(open("?token=not-a-jwt"), "invalid token")
    }

    @Test
    fun `closes a socket opened with an unsigned token`() {
        val unsigned = Jwts.builder().setSubject(alice.id.value).compact()

        assertRejected(open("?token=$unsigned"), "invalid token format")
    }

    @Test
    fun `closes a socket opened with a token signed by another key`() {
        val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private
        val forged = Jwts.builder().setSubject(alice.id.value).signWith(otherKey).compact()

        assertRejected(open("?token=$forged"), "signature validation failed")
    }

    @Test
    fun `closes a socket opened with an expired token`() {
        val expired = Jwts.builder()
            .setSubject(alice.id.value)
            .setExpiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(signingKey())
            .compact()

        assertRejected(open("?token=$expired"), "expired token")
    }

    @Test
    fun `closes a socket whose token belongs to a user that no longer exists`() {
        users.users.clear()

        assertRejected(open(), POLICY_VALIDATION.description)
    }

    @Test
    fun `accepts a socket whose token belongs to a user deactivated after it was issued`() {
        // Unlike the Authenticated filter on the REST routes, the console does not check the status.
        users.users.clear()
        users.users.add(alice.copy(status = INACTIVE))

        val websocket = open()

        assertNull(websocket.closedWith)
        assertTrue(minecraftServer.consoleSockets.contains(websocket))
    }

    @Test
    fun `answers a ping with a pong`() {
        val websocket = open()

        websocket.receive("""{"type":"ping"}""")

        assertEquals(listOf("""{"type":"pong"}"""), websocket.sent.map { it.bodyString() })
    }

    @Test
    fun `writes a command to the running server's console and audits it`() {
        val process = startFakeServer()
        val websocket = open()

        websocket.receive("""{"type":"command","command":"say hello"}""")

        assertEquals("say hello\n", process.input.toString(Charsets.UTF_8))
        assertEquals(listOf("Executed command \"say hello\""), audit.items.map { it.description })
        assertEquals(alice.id, audit.items.single().userId)
    }

    @Test
    fun `ignores a command while the server is not running`() {
        val process = FakeProcess()
        minecraftServer.process = process
        val websocket = open()

        websocket.receive("""{"type":"command","command":"say hello"}""")

        assertEquals("", process.input.toString(Charsets.UTF_8))
        assertTrue(audit.items.isEmpty())
    }

    @Test
    fun `ignores a start message while the server is already running`() {
        startFakeServer()
        val websocket = open()

        websocket.receive("""{"type":"start"}""")

        assertTrue(audit.items.isEmpty())
        assertTrue(websocket.sent.isEmpty())
    }

    @Test
    fun `unregisters the console socket when the client closes it`() {
        val websocket = open()

        websocket.clientCloses()

        assertFalse(minecraftServer.consoleSockets.contains(websocket))
    }

}
