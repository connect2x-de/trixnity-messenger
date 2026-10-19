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

package de.connect2x.livekitnity.compose.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import de.connect2x.livekitnity.compose.types.TrackReference

/** CompositionLocal for the [TrackReference] currently provided by [TrackReferenceLocal] */
@Suppress("CompositionLocalNaming")
public val TrackReferenceLocal: ProvidableCompositionLocal<TrackReference> = compositionLocalOf {
  throw IllegalStateException(
      "No Track object available. This should only be used within a TrackReferenceScope.",
  )
}

/** Binds [trackRef] to the [TrackReferenceLocal] for the scope of [content]. */
@Composable
public fun TrackReferenceScope(
    trackRef: TrackReference,
    content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
      TrackReferenceLocal provides trackRef,
      content = content,
  )
}

/**
 * Returns the [passedTrack] or the currently provided [TrackReferenceLocal].
 *
 * @throws IllegalStateException if passedTrack is null and no TrackReferenceLocal is available
 *   (e.g. not inside a [TrackReferenceScope]).
 */
@Composable
@Throws(IllegalStateException::class)
public fun requireTrack(passedTrack: TrackReference? = null): TrackReference {
  return passedTrack ?: TrackReferenceLocal.current
}

/**
 * A simple way to loop over tracks that creates a [TrackReferenceScope] for each track and calls
 * [content].
 */
@Composable
public fun ForEachTrack(
    tracks: List<TrackReference>,
    content: @Composable (TrackReference) -> Unit,
) {
  tracks.forEach { trackRef ->
    TrackReferenceScope(trackRef = trackRef) {
      content(trackRef)
    }
  }
}
