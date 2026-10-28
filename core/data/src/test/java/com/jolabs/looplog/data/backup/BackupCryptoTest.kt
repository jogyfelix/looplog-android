package com.jolabs.looplog.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BackupCryptoTest {

    private val crypto = BackupCrypto()
    private val payload = "hello looplog backup 🌱".toByteArray(Charsets.UTF_8)

    @Test
    fun `round-trips without a password`() {
        val encrypted = crypto.encrypt(payload, password = null)
        val decrypted = crypto.decrypt(encrypted, password = null)
        assertThat(decrypted).isEqualTo(payload)
    }

    @Test
    fun `round-trips with a password`() {
        val encrypted = crypto.encrypt(payload, password = "s3cret")
        val decrypted = crypto.decrypt(encrypted, password = "s3cret")
        assertThat(decrypted).isEqualTo(payload)
    }

    @Test
    fun `header reports password protection`() {
        assertThat(crypto.readHeader(crypto.encrypt(payload, "pw")).isPasswordProtected).isTrue()
        assertThat(crypto.readHeader(crypto.encrypt(payload, null)).isPasswordProtected).isFalse()
    }

    @Test
    fun `wrong password fails to decrypt`() {
        val encrypted = crypto.encrypt(payload, password = "correct")
        assertThrows<BackupDecryptException> { crypto.decrypt(encrypted, password = "wrong") }
    }

    @Test
    fun `missing password on a protected file fails`() {
        val encrypted = crypto.encrypt(payload, password = "correct")
        assertThrows<BackupDecryptException> { crypto.decrypt(encrypted, password = null) }
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val encrypted = crypto.encrypt(payload, password = null)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()
        assertThrows<BackupDecryptException> { crypto.decrypt(encrypted, password = null) }
    }

    @Test
    fun `tampered header fails authentication`() {
        val encrypted = crypto.encrypt(payload, password = null)
        // Flip a byte inside the salt (part of the authenticated header).
        encrypted[10] = (encrypted[10] + 1).toByte()
        assertThrows<BackupDecryptException> { crypto.decrypt(encrypted, password = null) }
    }

    @Test
    fun `foreign bytes are rejected as not-a-backup`() {
        assertThrows<NotABackupException> { crypto.readHeader("not a backup".toByteArray()) }
        assertThrows<NotABackupException> { crypto.decrypt("not a backup at all".toByteArray(), null) }
    }
}
