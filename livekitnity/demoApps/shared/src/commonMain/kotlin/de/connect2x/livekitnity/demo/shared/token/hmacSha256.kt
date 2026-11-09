package de.connect2x.livekitnity.demo.shared.token

internal expect suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
