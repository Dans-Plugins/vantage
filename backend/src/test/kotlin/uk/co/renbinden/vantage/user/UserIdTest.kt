package uk.co.renbinden.vantage.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class UserIdTest {

    @Test
    fun `generate produces a uuid string`() {
        val userId = UserId.generate()

        assertEquals(userId.value, UUID.fromString(userId.value).toString())
    }

    @Test
    fun `generate produces a distinct id on each call`() {
        assertNotEquals(UserId.generate(), UserId.generate())
    }

    @Test
    fun `ids wrapping the same value are equal`() {
        val value = UUID.randomUUID().toString()

        assertEquals(UserId(value), UserId(value))
        assertEquals(UserId(value).hashCode(), UserId(value).hashCode())
    }

    @Test
    fun `ids wrapping different values are not equal`() {
        assertNotEquals(UserId("a"), UserId("b"))
    }

    @Test
    fun `the wrapped value is exposed unchanged`() {
        assertEquals("a", UserId("a").value)
    }
}
