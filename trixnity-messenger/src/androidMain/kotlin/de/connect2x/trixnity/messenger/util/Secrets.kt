package de.connect2x.trixnity.messenger.util

import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import de.connect2x.trixnity.messenger.getContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

private val log = KotlinLogging.logger { }

actual suspend fun getSecret(id: String): String? {
    try {
        val masterKey = MasterKey.Builder(getContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        val file = File(getContext().filesDir, id)
        val encryptedFile = EncryptedFile.Builder(
            getContext(),
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        // write to the encrypted file
        val encryptedInputStream: FileInputStream = encryptedFile.openFileInput()
        log.info { "read secret ('$id') from encrypted file" }
        val password = encryptedInputStream.readBytes().decodeToString()
        encryptedInputStream.close()
        return password
    } catch (exc: IOException) {
        log.error(exc) {}
        log.warn { "Cannot find secret ('$id') file." }
    } catch (exc: Exception) {
        log.error(exc) { "Cannot read secret ('$id') file." }
    }

    return null
}

actual suspend fun setSecret(id: String, secret: String) {
    try {
        val masterKey = MasterKey.Builder(getContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        val file = File(getContext().filesDir, id)
        val encryptedFile = EncryptedFile.Builder(
            getContext(),
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        // write to the encrypted file
        val encryptedOutputStream: FileOutputStream = encryptedFile.openFileOutput()
        log.info { "write secret ('$id') to encrypted file" }
        encryptedOutputStream.write(secret.toByteArray())
        encryptedOutputStream.close()
    } catch (exc: Exception) {
        log.error(exc) { "Cannot write secret ('$id') to file." }
    }
}