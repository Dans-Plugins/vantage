package uk.co.renbinden.vantage.endpoint.api.v2.server

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.config.MinecraftConfig
import uk.co.renbinden.vantage.minecraft.MinecraftServer

class ServerHandlerTest {

    private val minecraftServer = MinecraftServer(MinecraftConfig())

    @Test
    fun `responds with isRunning false before the server has started`() {
        val response = ServerHandler(minecraftServer).get(Request(GET, "/api/v2/server"))

        assertEquals(OK, response.status)
        assertEquals("{\"isRunning\":false}", response.bodyString())
    }

    @Test
    fun `responds with isRunning true while the server is running`() {
        minecraftServer.isRunning = true

        val response = ServerHandler(minecraftServer).get(Request(GET, "/api/v2/server"))

        assertEquals(OK, response.status)
        assertEquals(ServerResponse(true), ServerResponse.lens(response))
    }

    @Test
    fun `reads the running state on every request`() {
        val handler = ServerHandler(minecraftServer)
        minecraftServer.isRunning = true
        handler.get(Request(GET, "/api/v2/server"))
        minecraftServer.isRunning = false

        val response = handler.get(Request(GET, "/api/v2/server"))

        assertEquals(ServerResponse(false), ServerResponse.lens(response))
    }

}
