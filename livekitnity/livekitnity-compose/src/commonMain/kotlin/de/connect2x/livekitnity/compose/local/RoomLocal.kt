/*
 * Copyright 2023-2025 LiveKit, Inc.
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

package de.connect2x.livekitnity.compose.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.livekitnity.api.ConnectOptions
import de.connect2x.livekitnity.api.RoomOptions
import de.connect2x.livekitnity.api.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A simple handler for listening to room state changes.
 *
 * @param states the types of states to listen to, or empty list to listen to all state changes.
 * @param passedRoom the room to use, or null to use [RoomLocal] if inside a [RoomScope].
 * @param onState the listener to be called back. Will be called immediately with the existing state
 *   if it is a matching state.
 * @param keys any keys that should be tracked to relaunch the handler with new keys.
 */
@Composable
public fun HandleRoomState(
    states: List<Room.State> = emptyList(),
    passedRoom: Room? = null,
    vararg keys: Any,
    onState: (suspend CoroutineScope.(Room, Room.State) -> Unit)?,
) {
  val room = requireRoom(passedRoom = passedRoom)

  if (onState != null) {
    val effectKeys = listOf(room, onState).plus(keys).toTypedArray()
    LaunchedEffect(*effectKeys) {
      launch {
        room.state.collectLatest { currentState ->
          if (states.isEmpty() || states.contains(currentState)) {
            onState.invoke(this, room, currentState)
          }
        }
      }
    }
  }
}

/** @see HandleRoomState */
@Composable
public fun HandleRoomState(
    state: Room.State,
    passedRoom: Room? = null,
    vararg keys: Any,
    onState: (suspend CoroutineScope.(Room, Room.State) -> Unit)?,
) {
  val states = listOf(state)
  HandleRoomState(
      states = states,
      passedRoom = passedRoom,
      keys = keys,
      onState = onState,
  )
}

private val DEFAULT_ERROR_HANDLER: ((Room, Exception?) -> Unit) = { _, e ->
  println("Error connecting to the server:")
  e?.printStackTrace()
}

/**
 * Remembers a new [Room] object.
 *
 * @param url the url of the livekit server to connect to.
 * @param token the token to connect to livekit with.
 * @param audio enable or disable audio. Defaults to false.
 * @param video enable or disable video. Defaults to false.
 * @param connect whether the room should be automatically connected to the server. Defaults to
 *   true.
 * @param roomOptions options to pass to the [Room].
 * @param connectOptions options to use when connecting. Will not reflect changes if already
 *   connected.
 * @param onConnected a listener to be called upon room connection.
 * @param onDisconnected a listener to be called upon room disconnection.
 * @param onError a listener to be called upon room error.
 * @param disconnectOnDispose by default, this composable handles the connection management and will
 *   disconnect the room when the composable goes out of scope. Setting this to false will disable
 *   this behavior. This is effective in combination (for example, with a background service).
 */
@Composable
public fun rememberLiveKitRoom(
    url: String? = null,
    token: String? = null,
    audio: Boolean = false,
    video: Boolean = false,
    connect: Boolean = true,
    roomOptions: RoomOptions? = null,
    connectOptions: ConnectOptions? = null,
    onConnected: (suspend CoroutineScope.(Room) -> Unit)? = null,
    onDisconnected: (suspend CoroutineScope.(Room) -> Unit)? = null,
    onError: ((Room, Exception?) -> Unit)? = DEFAULT_ERROR_HANDLER,
    disconnectOnDispose: Boolean = true,
): Room {
  val liveKit = LiveKitLocal.current

  val room =
      remember(
          roomOptions,
          roomOptions,
      ) {
        liveKit.create(
            options = roomOptions ?: RoomOptions(),
        )
      }

  HandleRoomState(Room.State.CONNECTED, room) { _, _ -> onConnected?.invoke(this, room) }

  // Only activate once per connect
  val audioOnce =
      remember(connect, audio) {
        var once = true
        val ret: suspend CoroutineScope.(Room, Room.State) -> Unit = { r: Room, _: Room.State ->
          if (once && audio) {
            once = false
            r.localParticipant.setMicrophoneEnabled(true)
          }
        }
        return@remember ret
      }
  HandleRoomState(
      state = Room.State.CONNECTED,
      passedRoom = room,
      keys = arrayOf(room, audioOnce),
      onState = audioOnce,
  )

  // Only activate once per connect
  val videoOnce =
      remember(connect, video) {
        var once = true
        val ret: suspend CoroutineScope.(Room, Room.State) -> Unit = { r: Room, _: Room.State ->
          if (once && video) {
            once = false
            r.localParticipant.setCameraEnabled(true)
          }
        }
        return@remember ret
      }
  HandleRoomState(
      state = Room.State.CONNECTED,
      passedRoom = room,
      keys = arrayOf(room, videoOnce),
      onState = videoOnce,
  )

  HandleRoomState(Room.State.DISCONNECTED, room) { _, _ -> onDisconnected?.invoke(this, room) }

  var hasConnected by
      remember(room) {
        mutableStateOf(false)
      }
  LaunchedEffect(room, connect, url, token, connectOptions) {
    // Only disconnect if we've connected before.
    if (!connect && hasConnected) {
      room.disconnect()
      return@LaunchedEffect
    }

    if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
      return@LaunchedEffect
    }

    try {
      hasConnected = true
      room.connect(url, token, connectOptions ?: ConnectOptions())
    } catch (e: Exception) {
      // onError?.invoke(room, RoomException.ConnectException(e.message, e))
      onError?.invoke(room, e)
    }
  }

  LaunchedEffect(room, connect) {
    try {
      awaitCancellation()
    } finally {
      if (connect && disconnectOnDispose) {
        room.disconnect()
      }
    }
  }
  LaunchedEffect(room) {
    try {
      awaitCancellation()
    } finally {
      room.disconnect()
      room.release()
    }
  }
  return room
}

/**
 * Establishes a room scope which remembers a [Room] object which can be accessed through the
 * [RoomLocal] composition local.
 *
 * @param url the url of the livekit server to connect to.
 * @param token the token to connect to livekit with.
 * @param audio enable or disable audio. Defaults to false.
 * @param video enable or disable video. Defaults to false.
 * @param connect whether the room should connect to the server. Defaults to true.
 * @param roomOptions options to pass to the [Room].
 * @param connectOptions options to use when connecting. Will not reflect changes if already
 *   connected.
 * @param onConnected a listener to be called upon room connection.
 * @param onDisconnected a listener to be called upon room disconnection.
 * @param onError a listener to be called upon room error.
 * @param disconnectOnDispose by default, this composable handles the connection management and will
 *   disconnect the room when the composable goes out of scope. Setting this to false will disable
 *   this behavior. This is effective in combination (for example, with a background service).
 */
@Composable
public fun RoomScope(
    url: String? = null,
    token: String? = null,
    audio: Boolean = false,
    video: Boolean = false,
    connect: Boolean = true,
    roomOptions: RoomOptions? = null,
    connectOptions: ConnectOptions? = null,
    onConnected: (suspend CoroutineScope.(Room) -> Unit)? = null,
    onDisconnected: (suspend CoroutineScope.(Room) -> Unit)? = null,
    onError: ((Room, Exception?) -> Unit)? = null,
    disconnectOnDispose: Boolean = true,
    content: @Composable (room: Room) -> Unit,
) {
  val room =
      rememberLiveKitRoom(
          url = url,
          token = token,
          audio = audio,
          video = video,
          connect = connect,
          roomOptions = roomOptions,
          connectOptions = connectOptions,
          onConnected = onConnected,
          onDisconnected = onDisconnected,
          onError = onError,
          disconnectOnDispose = disconnectOnDispose,
      )

  CompositionLocalProvider(
      RoomLocal provides room,
      content = { content(room) },
  )
}

/**
 * Returns the [passedRoom] or the currently provided [RoomLocal].
 *
 * @throws IllegalStateException if passedRoom is null and no RoomLocal is available (e.g. not
 *   inside a [RoomScope]).
 */
@Composable
@Throws(IllegalStateException::class)
public fun requireRoom(passedRoom: Room? = null): Room {
  return passedRoom ?: RoomLocal.current
}

/**
 * CompositionLocal for the [Room] currently provided by [RoomScope]
 *
 * Not to be confused with [LocalParticipant].
 */
@Suppress("CompositionLocalNaming")
public val RoomLocal: ProvidableCompositionLocal<Room> =
    compositionLocalOf<Room> {
      throw IllegalStateException(
          "No Room object available. This should only be used within a RoomScope.",
      )
    }
