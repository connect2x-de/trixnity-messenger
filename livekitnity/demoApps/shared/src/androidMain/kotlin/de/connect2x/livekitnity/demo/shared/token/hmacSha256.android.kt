package de.connect2x.livekitnity.demo.shared.token

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"

internal actual suspend fun hmacSha256(
    key: ByteArray,
    data: ByteArray,
): ByteArray {
  return withContext(Dispatchers.Default) {
    val secretKey = SecretKeySpec(key, HMAC_SHA256_ALGORITHM)
    val mac =
        Mac.getInstance(HMAC_SHA256_ALGORITHM).apply {
          init(secretKey)
        }
    mac.doFinal(data)
  }
}
