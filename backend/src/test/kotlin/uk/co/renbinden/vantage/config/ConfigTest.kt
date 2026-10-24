package uk.co.renbinden.vantage.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConfigTest {

    // Built the same way as the Gson that reads and writes config.json in Vantage.kt's main
    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun `defaults match the documented server defaults`() {
        val server = Config().server

        assertEquals("Server", server.name)
        assertEquals(9000, server.port)
        assertEquals("", server.baseUrl)
        assertEquals(listOf("http://localhost:3000"), server.allowedOrigins)
    }

    @Test
    fun `defaults match the documented database defaults`() {
        val database = Config().database

        assertEquals("jdbc:postgresql://localhost/vantage", database.url)
        assertNull(database.username)
        assertNull(database.password)
        assertEquals("POSTGRES", database.dialect)
    }

    @Test
    fun `defaults match the documented minecraft defaults`() {
        val minecraft = Config().minecraft

        assertEquals("java -jar spigot-1.19.2.jar -nogui", minecraft.command)
        assertEquals("./minecraft-server", minecraft.directory)
    }

    @Test
    fun `the generated config file omits the null database credentials`() {
        val database = JsonParser.parseString(gson.toJson(Config())).asJsonObject.getAsJsonObject("database")

        assertFalse(database.has("username"))
        assertFalse(database.has("password"))
        assertEquals("jdbc:postgresql://localhost/vantage", database.get("url").asString)
    }

    @Test
    fun `the generated config file reads back as the default config`() {
        assertEquals(Config(), gson.fromJson(gson.toJson(Config()), Config::class.java))
    }

    @Test
    fun `an empty object reads as the default config`() {
        assertEquals(Config(), gson.fromJson("{}", Config::class.java))
    }

    @Test
    fun `keys omitted from a section keep their defaults`() {
        val config = gson.fromJson("""{"server": {"port": 8080}}""", Config::class.java)

        assertEquals(ServerConfig(port = 8080), config.server)
        assertEquals(DatabaseConfig(), config.database)
        assertEquals(MinecraftConfig(), config.minecraft)
    }

    @Test
    fun `the documented example reads as written`() {
        val config = gson.fromJson(
            """
            {
              "server": {
                "name": "My Server",
                "port": 9000,
                "baseUrl": "https://panel.example.com",
                "allowedOrigins": [
                  "https://panel.example.com"
                ]
              },
              "database": {
                "url": "jdbc:postgresql://localhost/vantage",
                "username": "vantage",
                "password": "secret",
                "dialect": "POSTGRES"
              },
              "minecraft": {
                "command": "java -jar paper-1.19.2.jar -nogui",
                "directory": "/opt/minecraft"
              }
            }
            """.trimIndent(),
            Config::class.java
        )

        assertEquals(
            Config(
                ServerConfig("My Server", 9000, "https://panel.example.com", listOf("https://panel.example.com")),
                DatabaseConfig("jdbc:postgresql://localhost/vantage", "vantage", "secret", "POSTGRES"),
                MinecraftConfig("java -jar paper-1.19.2.jar -nogui", "/opt/minecraft")
            ),
            config
        )
    }

    @Test
    fun `an explicit null section is read as null despite the non-null type`() {
        val config = gson.fromJson("""{"server": null}""", Config::class.java)

        assertNull(config.javaClass.getDeclaredField("server").apply { isAccessible = true }.get(config))
    }

    @Test
    fun `unknown keys are ignored`() {
        assertEquals(Config(), gson.fromJson("""{"unknown": true, "server": {"colour": "red"}}""", Config::class.java))
    }

}
