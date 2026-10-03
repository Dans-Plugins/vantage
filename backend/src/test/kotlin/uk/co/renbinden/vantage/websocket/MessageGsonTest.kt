package uk.co.renbinden.vantage.websocket

import org.http4k.lens.LensFailure
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MessageGsonTest {

    private fun ServerboundMessage.toJson() = ServerboundMessage.lens(this).bodyString()
    private fun ClientboundMessage.toJson() = ClientboundMessage.lens(this).bodyString()
    private fun serverbound(json: String) = ServerboundMessage.lens(WsMessage(json))
    private fun clientbound(json: String) = ClientboundMessage.lens(WsMessage(json))

    @Test
    fun `command message is read with its command`() {
        val message = serverbound("""{"type":"command","command":"say hello"}""")

        assertTrue(message is CommandMessage)
        assertEquals("say hello", (message as CommandMessage).command)
    }

    @Test
    fun `start message is read as the start singleton`() {
        assertSame(StartMessage, serverbound("""{"type":"start"}"""))
    }

    @Test
    fun `ping message is read as the ping singleton`() {
        assertSame(PingMessage, serverbound("""{"type":"ping"}"""))
    }

    @Test
    fun `serverbound fields are read in any order`() {
        val message = serverbound("""{"command":"stop","type":"command"}""")

        assertEquals("stop", (message as CommandMessage).command)
    }

    @Test
    fun `serverbound message with an unknown type is rejected`() {
        assertThrows<LensFailure> { serverbound("""{"type":"stop"}""") }
    }

    @Test
    fun `serverbound message without a type is rejected`() {
        assertThrows<LensFailure> { serverbound("""{"command":"stop"}""") }
    }

    @Test
    fun `serverbound messages are written with their type`() {
        assertEquals("""{"type":"command","command":"say hello"}""", CommandMessage("say hello").toJson())
        assertEquals("""{"type":"start"}""", StartMessage.toJson())
        assertEquals("""{"type":"ping"}""", PingMessage.toJson())
    }

    @Test
    fun `pong message is written with only its type`() {
        assertEquals("""{"type":"pong"}""", PongMessage.toJson())
    }

    @Test
    fun `log message is written with its text`() {
        assertEquals("""{"type":"log","text":"[Server thread/INFO]: Done"}""", LogMessage("[Server thread/INFO]: Done").toJson())
    }

    @Test
    fun `status message is written with only its type`() {
        // The adapter does not write isRunning; the frontend refetches GET /api/v2/server on receipt.
        assertEquals("""{"type":"status"}""", ServerStatusMessage(true).toJson())
        assertEquals("""{"type":"status"}""", ServerStatusMessage(false).toJson())
    }

    @Test
    fun `clientbound messages are read by type`() {
        assertSame(PongMessage, clientbound("""{"type":"pong"}"""))
        assertEquals("line", (clientbound("""{"type":"log","text":"line"}""") as LogMessage).text)
        assertEquals(true, (clientbound("""{"type":"status","isRunning":true}""") as ServerStatusMessage).isRunning)
    }

    @Test
    fun `clientbound message with an unknown type is rejected`() {
        assertThrows<LensFailure> { clientbound("""{"type":"ping"}""") }
    }
}
