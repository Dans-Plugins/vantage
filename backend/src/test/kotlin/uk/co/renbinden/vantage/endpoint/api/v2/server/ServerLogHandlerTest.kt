package uk.co.renbinden.vantage.endpoint.api.v2.server

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.config.MinecraftConfig
import uk.co.renbinden.vantage.minecraft.MinecraftServer

class ServerLogHandlerTest {

    private val minecraftServer = MinecraftServer(MinecraftConfig())

    @Test
    fun `responds with an empty log before the server has written anything`() {
        val response = ServerLogHandler(minecraftServer).get(Request(GET, "/api/v2/server/log"))

        assertEquals(OK, response.status)
        assertEquals("{\"log\":[]}", response.bodyString())
    }

    @Test
    fun `responds with every console line, oldest first`() {
        minecraftServer.consoleLog.addAll(listOf("[Server] Starting", "[Server] Done (3.2s)!", "[Server] Stopping"))

        val response = ServerLogHandler(minecraftServer).get(Request(GET, "/api/v2/server/log"))

        assertEquals(OK, response.status)
        assertEquals(
            ServerLogResponse(listOf("[Server] Starting", "[Server] Done (3.2s)!", "[Server] Stopping")),
            ServerLogResponse.lens(response)
        )
    }

    @Test
    fun `does not trim a long log`() {
        val lines = (1..5000).map { "line $it" }
        minecraftServer.consoleLog.addAll(lines)

        val response = ServerLogHandler(minecraftServer).get(Request(GET, "/api/v2/server/log"))

        assertEquals(lines, ServerLogResponse.lens(response).log)
    }

    @Test
    fun `includes lines written after an earlier request`() {
        val handler = ServerLogHandler(minecraftServer)
        minecraftServer.consoleLog.add("first")
        handler.get(Request(GET, "/api/v2/server/log"))
        minecraftServer.consoleLog.add("second")

        val response = handler.get(Request(GET, "/api/v2/server/log"))

        assertEquals(listOf("first", "second"), ServerLogResponse.lens(response).log)
    }

}
