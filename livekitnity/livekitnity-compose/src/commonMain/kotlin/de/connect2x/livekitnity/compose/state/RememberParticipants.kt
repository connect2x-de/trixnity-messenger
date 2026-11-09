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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.participant.Participant
import de.connect2x.livekitnity.compose.local.requireRoom

/**
 * Remembers the full list of participants, with the local participant included as the first item in
 * the list.
 *
 * Updates automatically whenever the participant list changes.
 */
@Composable
public fun rememberParticipants(passedRoom: Room? = null): State<List<Participant>> {
  val room = requireRoom(passedRoom = passedRoom)

  val localParticipant = room.localParticipant
  val remoteParticipants by room.remoteParticipants.collectAsState()

  return remember {
    derivedStateOf {
      listOf(localParticipant).plus(remoteParticipants.values)
    }
  }
}

/**
 * Remembers a map from participant identity to participant, including the local participant.
 *
 * Updates whenever the participant list or an identity changes. Participants with a null identity
 * are omitted.
 */
@Composable
public fun rememberParticipantsByIdentity(
    passedRoom: Room? = null,
): State<Map<Participant.Identity, Participant>> {
  val participants by rememberParticipants(passedRoom = passedRoom)

  val participantsByIdentity =
      participants
          .mapNotNull { participant ->
            key(participant.sid) {
              val identity by participant.identity.collectAsState()
              identity?.let { it to participant }
            }
          }
          .toMap()

  return rememberUpdatedState(participantsByIdentity)
}
