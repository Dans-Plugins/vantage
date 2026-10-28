package uk.co.renbinden.vantage.endpoint.api.v2.files

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException

class FilesHandlerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var parentDirectory: File
    private lateinit var baseDirectory: File
    private lateinit var app: (Request) -> Response

    @BeforeEach
    fun setUp() {
        parentDirectory = File(tempDir, "parent").apply { mkdirs() }
        baseDirectory = File(parentDirectory, "minecraft").apply { mkdirs() }
        val handler = FilesHandler(baseDirectory)
        // Mirrors the /files routes in Vantage.kt, without the Authenticated filter.
        app = routes(
            "/" bind GET to handler::get,
            "/{path}" bind GET to handler::get,
            "/" bind PUT to handler::put,
            "/{path}" bind PUT to handler::put,
            "/{path}" bind DELETE to handler::delete
        )
    }

    @Test
    fun `get on the root lists directories first, then files, each by name`() {
        File(baseDirectory, "world").mkdir()
        File(baseDirectory, "logs").mkdir()
        File(baseDirectory, "server.properties").writeText("")
        File(baseDirectory, "eula.txt").writeText("")

        val response = app(Request(GET, "/"))

        assertEquals(OK, response.status)
        assertEquals(
            FilesResponse(
                listOf(
                    FileResponse("logs", true),
                    FileResponse("world", true),
                    FileResponse("eula.txt", false),
                    FileResponse("server.properties", false)
                )
            ),
            FilesResponse.lens(response)
        )
    }

    @Test
    fun `get on an empty directory lists nothing`() {
        val response = app(Request(GET, "/"))

        assertEquals(OK, response.status)
        assertEquals(FilesResponse(emptyList()), FilesResponse.lens(response))
    }

    @Test
    fun `get on a subdirectory lists its contents`() {
        File(baseDirectory, "plugins").mkdir()
        File(baseDirectory, "plugins/Vantage.jar").writeText("")

        val response = app(Request(GET, "/plugins"))

        assertEquals(OK, response.status)
        assertEquals(FilesResponse(listOf(FileResponse("Vantage.jar", false))), FilesResponse.lens(response))
    }

    @Test
    fun `get on a file returns its contents`() {
        File(baseDirectory, "server.properties").writeText("motd=A Minecraft Server")

        val response = app(Request(GET, "/server.properties"))

        assertEquals(OK, response.status)
        assertEquals("motd=A Minecraft Server", response.bodyString())
    }

    @Test
    fun `get with an encoded slash reads a file in a subdirectory`() {
        // The panel addresses nested paths as one path segment, encoding "/" as "%2F".
        File(baseDirectory, "plugins").mkdir()
        File(baseDirectory, "plugins/config.yml").writeText("enabled: true")

        val response = app(Request(GET, "/plugins%2Fconfig.yml"))

        assertEquals(OK, response.status)
        assertEquals("enabled: true", response.bodyString())
    }

    @Test
    fun `get on a missing file throws rather than returning not found`() {
        // Current behavior: the handler opens a FileInputStream without an existence check.
        assertThrows<FileNotFoundException> { app(Request(GET, "/missing.txt")) }
    }

    @Test
    fun `get with a dot-dot path currently lists the parent of the base directory`() {
        // Known defect: isInside compares un-normalized paths, so "minecraft/.." passes the
        // containment check. A fix should turn this into BAD_REQUEST.
        File(parentDirectory, "outside.txt").writeText("")

        val response = app(Request(GET, "/.."))

        assertEquals(OK, response.status)
        assertEquals(
            FilesResponse(listOf(FileResponse("minecraft", true), FileResponse("outside.txt", false))),
            FilesResponse.lens(response)
        )
    }

    @Test
    fun `put writes the request body to the named file`() {
        val response = app(Request(PUT, "/eula.txt").body("eula=true"))

        assertEquals(NO_CONTENT, response.status)
        assertEquals("eula=true", File(baseDirectory, "eula.txt").readText())
    }

    @Test
    fun `put replaces the contents of an existing file`() {
        File(baseDirectory, "eula.txt").writeText("eula=false")

        app(Request(PUT, "/eula.txt").body("eula=true"))

        assertEquals("eula=true", File(baseDirectory, "eula.txt").readText())
    }

    @Test
    fun `put with an encoded slash writes into an existing subdirectory`() {
        File(baseDirectory, "plugins").mkdir()

        val response = app(Request(PUT, "/plugins%2Fconfig.yml").body("enabled: true"))

        assertEquals(NO_CONTENT, response.status)
        assertEquals("enabled: true", File(baseDirectory, "plugins/config.yml").readText())
    }

    @Test
    fun `put into a missing subdirectory is rejected`() {
        val response = app(Request(PUT, "/plugins%2Fconfig.yml").body("enabled: true"))

        assertEquals(BAD_REQUEST, response.status)
        assertFalse(File(baseDirectory, "plugins").exists())
    }

    @Test
    fun `put on the root throws rather than returning bad request`() {
        // Current behavior: "minecraft/." passes the containment check, then writing bytes to a
        // directory fails.
        assertThrows<FileNotFoundException> { app(Request(PUT, "/").body("data")) }
    }

    @Test
    fun `delete removes a file`() {
        val file = File(baseDirectory, "eula.txt").apply { writeText("") }

        val response = app(Request(DELETE, "/eula.txt"))

        assertEquals(NO_CONTENT, response.status)
        assertFalse(file.exists())
    }

    @Test
    fun `delete removes a directory and its contents`() {
        val directory = File(baseDirectory, "world").apply { mkdir() }
        File(directory, "level.dat").writeText("")

        val response = app(Request(DELETE, "/world"))

        assertEquals(NO_CONTENT, response.status)
        assertFalse(directory.exists())
    }

    @Test
    fun `delete of a missing file still reports no content`() {
        val response = app(Request(DELETE, "/missing.txt"))

        assertEquals(NO_CONTENT, response.status)
    }

    @Test
    fun `delete with a dot-dot path currently removes the base directory itself`() {
        // Known defect: delete performs no containment check at all, unlike get and put, so
        // "minecraft/.." walks the parent directory. A fix should turn this into BAD_REQUEST and
        // leave the base directory in place.
        File(baseDirectory, "world").mkdir()
        File(baseDirectory, "world/level.dat").writeText("")

        val response = app(Request(DELETE, "/.."))

        assertEquals(NO_CONTENT, response.status)
        assertFalse(baseDirectory.exists())
    }
}
