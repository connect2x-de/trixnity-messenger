/*
 * Copyright 2023-2024 LiveKit, Inc.
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

package de.connect2x.livekitnity.compose.ui

import androidx.compose.ui.layout.LayoutCoordinates
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoSinkVisibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [VideoSinkVisibility] for compose views.
 *
 * To use, pass an `onGloballyPositioned` modifier your composable like so:
 *  ```
 *  modifier = Modifier.onGloballyPositioned { videoSinkVisibility.onGloballyPositioned(it) }
 *  ```
 *
 * [onDispose] must be called when the associated composable is no longer used (i.e. with a
 * DisposableEffect).
 *
 *  ```
 *  DisposableEffect(room) {
 *      onDispose {
 *          videoSinkVisibility.onDispose()
 *      }
 *  }
 *  ```
 */
internal class ComposeVisibility : VideoSinkVisibility {
  private var coordinates: LayoutCoordinates? = null

  private val _visibilityState = MutableStateFlow(calculateState())
  override val visibilityState: StateFlow<VideoSinkVisibility.State> =
      _visibilityState.asStateFlow()

  private fun calculateState(): VideoSinkVisibility.State {
    val coordinates =
        coordinates
            ?: return VideoSinkVisibility.State(
                isVisible = false,
                size = Track.Dimensions(0, 0),
            )

    val width = coordinates.size.width
    val height = coordinates.size.height
    val isAttached = coordinates.isAttached
    val isVisible = isAttached && width != 0 && height != 0

    return VideoSinkVisibility.State(
        isVisible = isVisible,
        size = Track.Dimensions(width, height),
    )
  }

  /**
   * To be called from a compose view, using `Modifier.onGloballyPositioned`.
   *
   * Example:
   * ```
   * modifier = Modifier.onGloballyPositioned { videoSinkVisibility.onGloballyPositioned(it) }
   * ```
   */
  fun onGloballyPositioned(layoutCoordinates: LayoutCoordinates) {
    coordinates = layoutCoordinates
    _visibilityState.value = calculateState()
  }

  /**
   * Cleans up any resources held by this object.
   *
   * Must be called upon leaving the composition (i.e. with a DisposableEffect).
   */
  fun onDispose() {
    if (coordinates == null) return
    coordinates = null
    _visibilityState.value = calculateState()
  }
}
