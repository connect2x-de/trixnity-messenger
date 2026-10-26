/*
 * Copyright 2025 LiveKit, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.connect2x.livekitnity.compose.types

import de.connect2x.livekitnity.api.CameraEnumerator
import de.connect2x.livekitnity.api.audio.AudioDevice

/**
 * A representation of all the media devices available to the local participant, as well as helper
 * methods for easier access for controlling those devices.
 */
public interface LocalMedia {
  /** The local participant's microphone track if one is published. */
  public val microphoneTrack: TrackReference?

  /** The local participant's camera track if one is published. */
  public val cameraTrack: TrackReference?

  /** The local participant's screen share track if one is published. */
  public val screenShareTrack: TrackReference?

  /** Whether the microphone is enabled. */
  public val isMicrophoneEnabled: Boolean

  /** Whether the camera is enabled. */
  public val isCameraEnabled: Boolean

  /** Whether screen sharing is enabled. */
  public val isScreenShareEnabled: Boolean

  /** The list of available audio devices. */
  public val audioDevices: List<AudioDevice>

  /** The list of available camera devices. */
  public val cameraDevices: List<String>

  /**
   * The current audio device in use, if it exists.
   *
   * @see selectAudioDevice
   */
  public val selectedAudioDevice: AudioDevice?

  /**
   * The id of the current camera in use, if it exists.
   *
   * @see selectCamera
   */
  public val selectedCameraId: String?

  /**
   * Whether the camera can switch position (i.e. from front facing to back facing camera, and
   * vice-versa).
   *
   * @see switchCamera
   */
  public val cameraCanSwitchPosition: Boolean

  // Should this be here?
  public val cameraEnumerator: CameraEnumerator

  /** Enables/disables the microphone track and publishes it if needed. */
  public suspend fun setMicrophoneEnabled(enabled: Boolean)

  /** Enables/disables the camera track and publishes it if needed. */
  public suspend fun setCameraEnabled(enabled: Boolean)

  /** Enables/disables screen share track and publishes it if needed. */
  public suspend fun setScreenShareEnabled(enabled: Boolean)

  /** Selects the audio device to be used. */
  public fun selectAudioDevice(audioDevice: AudioDevice)

  /** Selects the camera to be used. */
  public fun selectCamera(deviceName: String)

  /**
   * Switches the position of the camera, if able.
   *
   * @see switchCamera
   */
  public fun switchCamera()
}
