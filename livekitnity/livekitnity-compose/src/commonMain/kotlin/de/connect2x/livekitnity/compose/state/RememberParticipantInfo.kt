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

package de.connect2x.livekitnity.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.compose.local.requireParticipant

/** Holder for basic [Participant] information. */
@Stable
public class ParticipantInfo(
    nameState: State<String?>,
    identityState: State<Participant.Identity?>,
    metadataState: State<String?>,
) {
  public val name: String? by nameState
  public val identity: Participant.Identity? by identityState
  public val metadata: String? by metadataState
}

/** Remembers the participant info and updates whenever it is changed. */
@Composable
public fun rememberParticipantInfo(passedParticipant: Participant? = null): ParticipantInfo {
  val participant = requireParticipant(passedParticipant)

  val nameState = participant.name.collectAsState()
  val identityState = participant.identity.collectAsState()
  val metadataState = participant.metadata.collectAsState()

  val participantInfo =
      remember(nameState, identityState, metadataState) {
        ParticipantInfo(
            nameState = nameState,
            identityState = identityState,
            metadataState = metadataState,
        )
      }

  return participantInfo
}
