package de.connect2x.trixnity.messenger

import net.folivo.trixnity.core.model.RoomId
import org.koin.dsl.module

interface InitialRoom {
    val id: RoomId
}

fun MatrixMessenger.setInitialRoom(roomId: RoomId) {
    di.loadModules(listOf(module {
        single<InitialRoom> {
            object : InitialRoom {
                override val id: RoomId = roomId
            }
        }
    }), allowOverride = true, createEagerInstances = true)
}

fun MatrixMessenger.unsetInitialRoom() {
    di.getOrNull<InitialRoom>()?.apply {
        di.unloadModules(listOf(module {
            single<InitialRoom> { this@apply }
        }))
    }
}
