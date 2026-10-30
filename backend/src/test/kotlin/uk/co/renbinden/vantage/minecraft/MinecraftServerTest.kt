package uk.co.renbinden.vantage.minecraft

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.co.renbinden.vantage.config.MinecraftConfig
import uk.co.renbinden.vantage.websocket.ClientboundMessage
import uk.co.renbinden.vantage.websocket.LogMessage
import java.io.File
import java.util.Collections
import kotlin.concurrent.thread

@Timeout(60)
class MinecraftServerTest {

    @TempDir
    lateinit var root: File

    private lateinit var java: String
    private lateinit var fakeServerSource: File
    private lateinit var directory: File

    /** Records what [MinecraftServer] sends to a console socket; sends arrive from more than one thread. */
    private class FakeWebsocket : Websocket {
        override val upgradeRequest = Request(GET, "/ws/log")
        val sent: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override fun send(message: WsMessage) {
            sent.add(message.bodyString())
        }

        // A status message is written without isRunning, so only log messages can be read back.
        fun statuses() = sent.toList().filter { it == """{"type":"status"}""" }

        fun logLines() = sent.toList()
            .filter { it != """{"type":"status"}""" }
            .map { (ClientboundMessage.lens(WsMessage(it)) as LogMessage).text }

        override fun close(status: WsStatus) {}
        override fun onError(fn: (Throwable) -> Unit) {}
        override fun onClose(fn: (WsStatus) -> Unit) {}
        override fun onMessage(fn: (WsMessage) -> Unit) {}
    }

    @BeforeEach
    fun setUp() {
        // Stands in for the Minecraft server: a single-file Java program run by this JVM's own launcher.
        java = File(System.getProperty("java.home"), "bin/java").path
        fakeServerSource = File(root, "FakeMinecraftServer.java")
        fakeServerSource.writeText(
            """
            import java.io.BufferedReader;
            import java.io.File;
            import java.io.InputStreamReader;

            public class FakeMinecraftServer {
                public static void main(String[] args) throws Exception {
                    System.err.println("a line on standard error");
                    System.err.flush();
                    System.out.println("dir: " + new File(".").getCanonicalPath());
                    boolean untilStop = false;
                    for (String arg : args) {
                        if (arg.equals("--until-stop")) {
                            untilStop = true;
                        } else {
                            System.out.println("arg: " + arg);
                        }
                    }
                    System.out.println("Done");
                    System.out.flush();
                    if (untilStop) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                        String line;
                        while ((line = in.readLine()) != null && !line.equals("stop")) {}
                        System.out.println("Stopping server");
                    }
                }
            }
            """.trimIndent()
        )
        directory = File(root, "server")
        // MinecraftServer splits the command on whitespace, so neither path may contain any.
        assumeFalse(Regex("\\s").containsMatchIn(java + fakeServerSource.path + directory.path))
    }

    private fun server(arguments: String = "", directory: File = this.directory) = MinecraftServer(
        MinecraftConfig(
            command = "$java ${fakeServerSource.path} $arguments".trim(),
            directory = directory.path
        )
    )

    /** Lines reach consoleLog on a reader thread, which can still be running when start returns. */
    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 30_000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("condition not met within 30 seconds")
            Thread.sleep(10)
        }
    }

    @Test
    fun `records each line the process writes to standard output, in order`() {
        val server = server("one")

        server.start()
        awaitUntil { "Done" in server.consoleLog }

        assertEquals(
            listOf("dir: ${directory.canonicalPath}", "arg: one", "Done"),
            server.consoleLog.toList()
        )
    }

    @Test
    fun `does not record standard error`() {
        val server = server()

        server.start()
        awaitUntil { "Done" in server.consoleLog }

        assertFalse("a line on standard error" in server.consoleLog)
    }

    @Test
    fun `runs the command in the configured directory, creating it if it does not exist`() {
        val nested = File(root, "a/b")
        val server = server(directory = nested)

        server.start()
        awaitUntil { "Done" in server.consoleLog }

        assertTrue(nested.isDirectory)
        assertEquals("dir: ${nested.canonicalPath}", server.consoleLog.first())
    }

    @Test
    fun `splits the command on runs of whitespace`() {
        val server = server("one  \t two")

        server.start()
        awaitUntil { "Done" in server.consoleLog }

        assertEquals(listOf("arg: one", "arg: two"), server.consoleLog.filter { it.startsWith("arg: ") })
    }

    @Test
    fun `refuses to start when the configured directory is a file`() {
        val file = File(root, "server")
        file.writeText("not a directory")
        val server = server()

        val exception = assertThrows<Exception> { server.start() }

        assertEquals("Please remove the file at ${file.canonicalPath}", exception.message)
        assertNull(server.process)
        assertFalse(server.isRunning)
    }

    @Test
    fun `blocks while the process runs and is no longer running once it exits`() {
        val server = server("--until-stop")
        val starter = thread { server.start() }

        awaitUntil { "Done" in server.consoleLog }
        assertTrue(server.isRunning)
        assertTrue(starter.isAlive)

        // The process's standard input is how the console and the shutdown hook reach the server.
        val input = server.process!!.outputStream
        input.write("stop\n".toByteArray())
        input.flush()
        starter.join()

        assertFalse(server.isRunning)
        assertEquals(0, server.process!!.exitValue())
        awaitUntil { "Stopping server" in server.consoleLog }
    }

    @Test
    fun `sends console sockets a status message on start and on exit, and each line as a log message`() {
        val server = server()
        val socket = FakeWebsocket()
        server.consoleSockets.add(socket)

        server.start()
        awaitUntil { socket.logLines().size == 2 }

        // The exit status is sent from start's thread and log lines from the reader thread, so the
        // relative order of the last line and the exit status is not fixed.
        assertEquals("""{"type":"status"}""", socket.sent.first())
        assertEquals(2, socket.statuses().size)
        assertEquals(listOf("dir: ${directory.canonicalPath}", "Done"), socket.logLines())
    }

}
