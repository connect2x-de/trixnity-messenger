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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import de.connect2x.livekitnity.api.room.participant.Participant

/**
 * CompositionLocal for the [Participant] currently provided by [ParticipantScope]
 *
 * Not to be confused with [LocalParticipant].
 */
@Suppress("CompositionLocalNaming")
public val ParticipantLocal: ProvidableCompositionLocal<Participant> = compositionLocalOf {
  throw IllegalStateException(
      "No Participant object available. This should only be used within a ParticipantScope.",
  )
}

/**
 * Wraps [content] with a [CompositionLocalProvider] that binds [participant] to [ParticipantLocal].
 */
@Composable
public fun ParticipantScope(
    participant: Participant,
    content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
      ParticipantLocal provides participant,
      content = content,
  )
}

/**
 * Returns the [passedParticipant] or the currently provided [ParticipantLocal].
 *
 * @throws IllegalStateException if passedParticipant is null and no ParticipantLocal is available
 *   (e.g. not inside a [ParticipantScope]).
 */
@Composable
@Throws(IllegalStateException::class)
public fun requireParticipant(passedParticipant: Participant? = null): Participant {
  return passedParticipant ?: ParticipantLocal.current
}

/**
 * A simple way to loop over participants that creates a [ParticipantScope] for each participant and
 * calls [content].
 */
@Composable
public fun ForEachParticipant(
    participants: List<Participant>,
    content: @Composable (Participant) -> Unit,
) {
  participants.forEach { participant ->
    ParticipantScope(participant = participant) {
      content(participant)
    }
  }
}
