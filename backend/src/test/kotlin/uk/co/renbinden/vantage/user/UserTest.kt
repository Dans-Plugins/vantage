package uk.co.renbinden.vantage.user

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `new user is active at version zero`() {
        val user = User("alice", "correct horse battery staple")

        assertEquals("alice", user.username)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(0, user.version)
    }

    @Test
    fun `new user gets a sixteen byte salt and a sixteen byte hash`() {
        val user = User("alice", "correct horse battery staple")

        assertEquals(16, user.passwordSalt.size)
        assertEquals(16, user.passwordHash.size)
    }

    @Test
    fun `new user gets a generated id`() {
        val user = User("alice", "correct horse battery staple")
        val otherUser = User("alice", "correct horse battery staple")

        assertNotEquals(user.id, otherUser.id)
    }

    @Test
    fun `check password accepts the password the user was created with`() {
        val user = User("alice", "correct horse battery staple")

        assertTrue(user.checkPassword("correct horse battery staple"))
    }

    @Test
    fun `check password rejects a different password`() {
        val user = User("alice", "correct horse battery staple")

        assertFalse(user.checkPassword("incorrect horse battery staple"))
    }

    @Test
    fun `check password is case sensitive`() {
        val user = User("alice", "correct horse battery staple")

        assertFalse(user.checkPassword("Correct Horse Battery Staple"))
    }

    @Test
    fun `two users with the same password get different salts and different hashes`() {
        val user = User("alice", "correct horse battery staple")
        val otherUser = User("bob", "correct horse battery staple")

        assertFalse(user.passwordSalt.contentEquals(otherUser.passwordSalt))
        assertFalse(user.passwordHash.contentEquals(otherUser.passwordHash))
    }

    @Test
    fun `with password replaces the hash and the salt`() {
        val user = User("alice", "correct horse battery staple")

        val updatedUser = user.withPassword("a different password")

        assertFalse(user.passwordSalt.contentEquals(updatedUser.passwordSalt))
        assertFalse(user.passwordHash.contentEquals(updatedUser.passwordHash))
    }

    @Test
    fun `with password keeps every other field`() {
        val user = User("alice", "correct horse battery staple")

        val updatedUser = user.withPassword("a different password")

        assertEquals(user.id, updatedUser.id)
        assertEquals(user.version, updatedUser.version)
        assertEquals(user.username, updatedUser.username)
        assertEquals(user.status, updatedUser.status)
    }

    @Test
    fun `with password leaves the original user unchanged`() {
        val user = User("alice", "correct horse battery staple")

        user.withPassword("a different password")

        assertTrue(user.checkPassword("correct horse battery staple"))
    }

    @Test
    fun `check password on a user returned by with password accepts only the new password`() {
        val user = User("alice", "correct horse battery staple")

        val updatedUser = user.withPassword("a different password")

        assertTrue(updatedUser.checkPassword("a different password"))
        assertFalse(updatedUser.checkPassword("correct horse battery staple"))
    }

    @Test
    fun `copy without arguments produces an equal user`() {
        val user = User("alice", "correct horse battery staple")

        val copiedUser = user.copy()

        assertEquals(user, copiedUser)
        assertEquals(user.hashCode(), copiedUser.hashCode())
    }

    @Test
    fun `copy replaces only the named field`() {
        val user = User("alice", "correct horse battery staple")

        val copiedUser = user.copy(username = "bob")

        assertEquals("bob", copiedUser.username)
        assertEquals(user.id, copiedUser.id)
        assertEquals(user.version, copiedUser.version)
        assertEquals(user.status, copiedUser.status)
        assertArrayEquals(user.passwordHash, copiedUser.passwordHash)
        assertArrayEquals(user.passwordSalt, copiedUser.passwordSalt)
    }

    @Test
    fun `copy can change the status`() {
        val user = User("alice", "correct horse battery staple")

        val copiedUser = user.copy(status = UserStatus.INACTIVE)

        assertEquals(UserStatus.INACTIVE, copiedUser.status)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `equals compares password bytes by content rather than by reference`() {
        val user = User("alice", "correct horse battery staple")

        val equivalentUser = User(
            user.id,
            user.version,
            user.username,
            user.passwordHash.copyOf(),
            user.passwordSalt.copyOf(),
            user.status
        )

        assertEquals(user, equivalentUser)
        assertEquals(user.hashCode(), equivalentUser.hashCode())
    }

    @Test
    fun `users differing in a single field are not equal`() {
        val user = User("alice", "correct horse battery staple")

        assertNotEquals(user, user.copy(username = "bob"))
        assertNotEquals(user, user.copy(version = user.version + 1))
        assertNotEquals(user, user.copy(id = UserId.generate()))
        assertNotEquals(user, user.copy(status = UserStatus.INACTIVE))
        assertNotEquals(user, user.copy(passwordHash = ByteArray(16)))
        assertNotEquals(user, user.copy(passwordSalt = ByteArray(16)))
    }

    @Test
    fun `a user is not equal to a value of another type`() {
        val user = User("alice", "correct horse battery staple")

        assertNotEquals(user, "alice")
    }
}
