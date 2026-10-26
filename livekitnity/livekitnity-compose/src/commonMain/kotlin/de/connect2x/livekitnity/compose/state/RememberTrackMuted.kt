/*
 * Copyright 2024-2025 LiveKit, Inc.
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.util.rememberStateOrDefault

/** @return true if the referenced track is muted or is a placeholder */
@Composable
public fun rememberTrackMuted(trackRef: TrackReference): State<Boolean> {
  return rememberStateOrDefault(true) {
    val publication = trackRef.publication
    publication?.muted?.collectAsState()
  }
}
