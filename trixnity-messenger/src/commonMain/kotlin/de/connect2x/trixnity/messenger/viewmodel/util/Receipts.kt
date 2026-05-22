package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.flattenNotNull
import de.connect2x.trixnity.client.store.RoomUserReceipts
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.ReceiptType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

fun Flow<Map<UserId, Flow<RoomUserReceipts?>>>.byEventId(filterOwnUserId: UserId? = null) =
    flattenNotNull()
        .map { receipts ->
            receipts
                .mapNotNull { (key, value) ->
                    if (key == filterOwnUserId) null else value.receipts[ReceiptType.Read]?.let { it.eventId to key }
                }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.toSet() }
        }
        .distinctUntilChanged()
