package de.connect2x.livekitnity.demo.shared.token

public interface LiveKitTokenGenerator {
  public suspend fun createToken(options: Options): String

  public data class Options(
      val apiKey: String = "devkey",
      val apiSecret: String = "secret",
      val identity: String,
      val roomName: String,
      val ttlSeconds: Long = 3600L,
      val canPublish: Boolean = true,
      val canSubscribe: Boolean = true,
  )
}
