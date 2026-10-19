package de.connect2x.livekitnity.demo.shared.token

import kotlin.io.encoding.Base64
import kotlin.time.Clock

public class DefaultLiveKitTokenGenerator : LiveKitTokenGenerator {

  private val base64UrlEncoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

  override suspend fun createToken(options: LiveKitTokenGenerator.Options): String {
    val now = Clock.System.now().epochSeconds
    val exp = now + options.ttlSeconds

    val headerJson = """{"alg":"HS256","typ":"JWT"}"""
    val payloadJson =
        """
            {
              "iss": "${options.apiKey}",
              "sub": "${options.identity}",
              "nbf": $now,
              "exp": $exp,
              "video": {
                "room": "${options.roomName}",
                "roomJoin": true,
                "canPublish": ${options.canPublish},
                "canSubscribe": ${options.canSubscribe}
              }
            }
        """
            .trimIndent()
            .replace("\\s+".toRegex(), "")

    val headerEncoded = base64UrlEncoder.encode(headerJson.encodeToByteArray())
    val payloadEncoded = base64UrlEncoder.encode(payloadJson.encodeToByteArray())
    val signingInput = "$headerEncoded.$payloadEncoded"

    val signatureBytes =
        hmacSha256(
            key = options.apiSecret.encodeToByteArray(),
            data = signingInput.encodeToByteArray(),
        )
    val signatureEncoded = base64UrlEncoder.encode(signatureBytes)

    return "$signingInput.$signatureEncoded"
  }
}
