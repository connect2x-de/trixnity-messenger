package de.connect2x.trixnity.messenger

import androidx.room.Room
import androidx.room.RoomDatabase
import de.connect2x.trixnity.messenger.util.ContextGetter
import org.koin.core.scope.Scope

internal actual inline fun <reified T : RoomDatabase> Scope.roomDatabaseBuilder(name: String): RoomDatabase.Builder<T> =
    Room.databaseBuilder(get<ContextGetter>()(), name)
