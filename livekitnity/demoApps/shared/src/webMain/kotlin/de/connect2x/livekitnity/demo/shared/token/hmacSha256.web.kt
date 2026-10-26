@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.livekitnity.demo.shared.token

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array

internal actual suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
  val result =
      hmacSha256Impl(
          key = key.toInt8Array(),
          data = data.toInt8Array(),
      )

  return suspendCancellableCoroutine { continuation ->
    result
        .then {
          continuation.resume(it.toByteArray())
          null
        }
        .catch {
          continuation.resumeWithException(it.asJsException())
          null
        }
  }
}

@JsFun(
    """
    (keyBytes, dataBytes) => {
      const cryptoKeyPromise = globalThis.crypto.subtle.importKey(
        "raw",
        keyBytes,
        { name: "HMAC", hash: "SHA-256" },
        false,
        ["sign"],
      );
            
      const signatureBufferPromise = cryptoKeyPromise.then(cryptoKey => {
        return globalThis.crypto.subtle.sign(
          "HMAC",
          cryptoKey,
          dataBytes,
        );
      }); 
      
      return signatureBufferPromise.then(signatureBuffer => {
        return new Int8Array(signatureBuffer);
      });
    }
  """,
)
private external fun hmacSha256Impl(key: Int8Array, data: Int8Array): Promise<Int8Array>
