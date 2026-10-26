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

package de.connect2x.livekitnity.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import de.connect2x.livekitnity.api.CameraEnumerator
import de.connect2x.livekitnity.api.audio.AudioDevice
import de.connect2x.livekitnity.api.audio.AudioDeviceChangeListener
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.CameraPosition
import de.connect2x.livekitnity.api.room.track.LocalVideoTrack
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.local.LiveKitLocal
import de.connect2x.livekitnity.compose.local.requireRoom
import de.connect2x.livekitnity.compose.types.LocalMedia
import de.connect2x.livekitnity.compose.types.TrackReference

internal class LocalMediaImpl(
    microphoneTrackState: State<TrackReference?>,
    cameraTrackState: State<TrackReference?>,
    screenShareTrackState: State<TrackReference?>,
    isMicrophoneEnabledState: State<Boolean>,
    isCameraEnabledState: State<Boolean>,
    isScreenShareEnabledState: State<Boolean>,
    audioDevicesState: SnapshotStateList<AudioDevice>,
    cameraDevicesState: SnapshotStateList<String>,
    selectedAudioDeviceState: State<AudioDevice?>,
    selectedCameraState: State<String?>,
    canSwitchPositionState: State<Boolean>,
    private val setMicrophoneFn: suspend (Boolean) -> Boolean,
    private val setCameraFn: suspend (Boolean) -> Boolean,
    private val setScreenShareFn: suspend (Boolean) -> Boolean,
    private val selectAudioDeviceFn: (AudioDevice) -> Unit?,
    private val selectCameraFn: (String) -> Unit?,
    private val switchCameraFn: () -> Unit?,
    override val cameraEnumerator: CameraEnumerator,
) : LocalMedia {
  override val microphoneTrack by microphoneTrackState
  override val cameraTrack by cameraTrackState
  override val screenShareTrack by screenShareTrackState

  override val isMicrophoneEnabled by isMicrophoneEnabledState
  override val isCameraEnabled by isCameraEnabledState
  override val isScreenShareEnabled by isScreenShareEnabledState

  override val audioDevices = audioDevicesState
  override val cameraDevices = cameraDevicesState

  override val selectedAudioDevice by selectedAudioDeviceState
  override val selectedCameraId by selectedCameraState
  override val cameraCanSwitchPosition by canSwitchPositionState

  override suspend fun setMicrophoneEnabled(enabled: Boolean) {
    setMicrophoneFn(enabled)
  }

  override suspend fun setCameraEnabled(enabled: Boolean) {
    setCameraFn(enabled)
  }

  override suspend fun setScreenShareEnabled(enabled: Boolean) {
    setScreenShareFn(enabled)
  }

  override fun selectAudioDevice(audioDevice: AudioDevice) {
    selectAudioDeviceFn(audioDevice)
  }

  override fun selectCamera(deviceName: String) {
    selectCameraFn(deviceName)
  }

  override fun switchCamera() {
    switchCameraFn()
  }
}

@Composable
public fun rememberLocalMedia(room: Room? = null): LocalMedia {
  val room = requireRoom(room)

  val localMicTrack by
      rememberParticipantTrackReferences(
          sources = listOf(Track.Source.MICROPHONE),
          passedParticipant = room.localParticipant,
      )
  val micState = remember {
    derivedStateOf { localMicTrack.firstOrNull() }
  }

  val localCameraTrack by
      rememberParticipantTrackReferences(
          sources = listOf(Track.Source.CAMERA),
          passedParticipant = room.localParticipant,
      )
  val cameraState = remember {
    derivedStateOf { localCameraTrack.firstOrNull() }
  }

  val localScreenShareTrack by
      rememberParticipantTrackReferences(
          sources = listOf(Track.Source.SCREEN_SHARE),
          passedParticipant = room.localParticipant,
      )
  val screenShareState = remember {
    derivedStateOf { localScreenShareTrack.firstOrNull() }
  }

  val isMicrophoneEnabledState = room.localParticipant.isMicrophoneEnabled.collectAsState()
  val isCameraEnabledState = room.localParticipant.isCameraEnabled.collectAsState()
  val isScreenShareEnabledState = room.localParticipant.isScreenShareEnabled.collectAsState()

  // Audio
  val selectedAudioDeviceState = remember {
    mutableStateOf(room.audioSwitchHandler?.selectedAudioDevice)
  }
  val audioDevicesStateList = remember {
    mutableStateListOf(
        *(room.audioSwitchHandler?.availableAudioDevices?.toTypedArray() ?: emptyArray()),
    )
  }

  DisposableEffect(room) {
    val audioSwitchHandler = room.audioSwitchHandler
    val audioDeviceChangeListener: AudioDeviceChangeListener = { audioDevices, selectedDevice ->
      audioDevicesStateList.clear()
      audioDevicesStateList.addAll(audioDevices)

      selectedAudioDeviceState.value = selectedDevice
    }
    if (audioSwitchHandler != null) {
      audioSwitchHandler.registerAudioDeviceChangeListener(audioDeviceChangeListener)
    } else {
      println("Room.audioSwitchHandler is null. Audio management is unavailable.")
    }

    onDispose {
      audioSwitchHandler?.unregisterAudioDeviceChangeListener(audioDeviceChangeListener)
    }
  }

  val selectAudioDeviceFn =
      remember(room) {
        { audioDevice: AudioDevice ->
          val audioSwitchHandler = room.audioSwitchHandler
          audioSwitchHandler?.selectDevice(audioDevice)
        }
      }

  val liveKit = LiveKitLocal.current
  val enumerator = remember {
    liveKit.createCameraEnumerator()
  }
  val availableCameras = remember {
    val devices = enumerator.deviceNames
    mutableStateListOf(*devices.toTypedArray())
  }
  val selectedCameraState = remember {
    derivedStateOf {
      val cameraTrack = cameraState.value?.publication?.track as? LocalVideoTrack
      cameraTrack?.deviceId
    }
  }

  val canSwitchPositionState = remember {
    derivedStateOf {
      val deviceName = selectedCameraState.value
      if (deviceName == null) {
        false
      } else {
        enumerator.isBackFacing(deviceName) || enumerator.isFrontFacing(deviceName)
      }
    }
  }
  val selectCameraFn =
      remember(room) {
        { deviceName: String ->
          val cameraTrack = cameraState.value?.publication?.track as? LocalVideoTrack
          cameraTrack?.switchCamera(deviceId = deviceName)
        }
      }
  val switchCameraFn =
      remember(room) {
        {
          val cameraTrack = cameraState.value?.publication?.track as? LocalVideoTrack
          if (cameraTrack != null) {
            val newPosition = cameraTrack.position?.flipped()
            if (newPosition != null) {
              cameraTrack.switchCamera(position = newPosition)
            }
          }
        }
      }

  val setMicrophoneFn =
      remember(room) {
        val fn: suspend (Boolean) -> Boolean = { enabled: Boolean ->
          room.localParticipant.setMicrophoneEnabled(enabled)
        }
        return@remember fn
      }
  val setCameraFn =
      remember(room) {
        val fn: suspend (Boolean) -> Boolean = { enabled: Boolean ->
          room.localParticipant.setCameraEnabled(enabled)
        }
        return@remember fn
      }
  val setScreenShareFn =
      remember(room) {
        val fn: suspend (Boolean) -> Boolean = { enabled ->
          room.localParticipant.setScreenShareEnabled(enabled)
        }
        return@remember fn
      }

  val localMedia =
      remember(room) {
        LocalMediaImpl(
            microphoneTrackState = micState,
            cameraTrackState = cameraState,
            screenShareTrackState = screenShareState,
            audioDevicesState = audioDevicesStateList,
            cameraDevicesState = availableCameras,
            selectedAudioDeviceState = selectedAudioDeviceState,
            selectedCameraState = selectedCameraState,
            canSwitchPositionState = canSwitchPositionState,
            setMicrophoneFn = setMicrophoneFn,
            setCameraFn = setCameraFn,
            setScreenShareFn = setScreenShareFn,
            selectAudioDeviceFn = selectAudioDeviceFn,
            selectCameraFn = selectCameraFn,
            switchCameraFn = switchCameraFn,
            cameraEnumerator = enumerator,
            isMicrophoneEnabledState = isMicrophoneEnabledState,
            isCameraEnabledState = isCameraEnabledState,
            isScreenShareEnabledState = isScreenShareEnabledState,
        )
      }

  return localMedia
}

private fun CameraPosition.flipped(): CameraPosition {
  return when (this) {
    CameraPosition.FRONT -> CameraPosition.BACK
    CameraPosition.BACK -> CameraPosition.FRONT
  }
}
