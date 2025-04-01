package de.connect2x.trixnity.messenger.secrets

import net.folivo.trixnity.crypto.core.hmacSha256

internal suspend fun hkdfSha256(
    key: ByteArray,
    salt: ByteArray,
    info: ByteArray = ByteArray(0),
    keyBytesLength: Int,
): ByteArray {
    val hashLen = 32
    val iterations = (keyBytesLength + hashLen - 1) / hashLen
    require(iterations <= 255) { "keyBytesLength must be less then 255 * $hashLen bytes, was $keyBytesLength bits" }

    val hkdfKey = hmacSha256(salt.takeIf { it.isNotEmpty() } ?: ByteArray(hashLen), key)

    /**
     * The output `OKM` is calculated as follows:
     *
     *    N = ceil(L/HashLen) (iterations)
     *    T = T(1) | T(2) | T(3) | ... | T(N) (block)
     *    OKM = first L octets of T (output)
     *
     *    where:
     *    T(0) = empty string (zero length)
     *    T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
     *    T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
     *    T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
     *    ...
     */
    val output = ByteArray(iterations * hashLen)
    val iterationArray = ByteArray(1)
    var t = ByteArray(0)
    repeat(iterations) { iteration ->
        iterationArray[0] = (iteration + 1).toByte()
        val data = t + info + iterationArray
        t = hmacSha256(hkdfKey, data)
        t.copyInto(output, hashLen * iteration)
    }

    return output.copyOf(keyBytesLength)
}
