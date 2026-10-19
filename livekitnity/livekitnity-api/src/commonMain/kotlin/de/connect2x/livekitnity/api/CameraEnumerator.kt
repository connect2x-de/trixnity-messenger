package de.connect2x.livekitnity.api

public interface CameraEnumerator {
  public val deviceNames: List<String>

  public fun isBackFacing(deviceName: String): Boolean

  public fun isFrontFacing(deviceName: String): Boolean
}
