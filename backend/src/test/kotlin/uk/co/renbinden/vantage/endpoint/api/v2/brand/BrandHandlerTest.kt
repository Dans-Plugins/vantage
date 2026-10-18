package uk.co.renbinden.vantage.endpoint.api.v2.brand

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.co.renbinden.vantage.config.ServerConfig

class BrandHandlerTest {

    @Test
    fun `responds with the configured server name as serverName`() {
        val response = BrandHandler(ServerConfig(name = "Kingdoms")).get(Request(GET, "/api/v2/brand"))

        assertEquals(OK, response.status)
        assertEquals("{\"serverName\":\"Kingdoms\"}", response.bodyString())
    }

    @Test
    fun `responds with Server when the name is not configured`() {
        val response = BrandHandler(ServerConfig()).get(Request(GET, "/api/v2/brand"))

        assertEquals(BrandResponse("Server"), BrandResponse.lens(response))
    }

}
