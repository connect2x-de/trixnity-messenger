package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.store.RoomUserReceipts
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReceiptType

fun Flow<Map<UserId, Flow<RoomUserReceipts?>>>.byEventId(filterOwnUserId: UserId? = null) =
    flattenNotNull()
        .map { receipts ->
            receipts
                .mapNotNull { (key, value) ->
                    if (key == filterOwnUserId) null
                    else value.receipts[ReceiptType.Read]
                        ?.let { it.eventId to key }
                }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.toSet() }
        }.distinctUntilChanged()
