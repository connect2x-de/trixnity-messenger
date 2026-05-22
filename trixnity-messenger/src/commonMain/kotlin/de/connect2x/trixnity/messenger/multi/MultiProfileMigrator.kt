package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.Worker

// TODO remove after a migration period

interface MultiProfileMigrator : Worker

class MultiProfileMigratorImpl(val profileManager: ProfileManager) : MultiProfileMigrator {
    override suspend fun doWork() {
        if (profileManager.isMultiProfileEnabled.value == null) {
            // deduce whether we should migrate to multi-profile mode
            if (profileManager.profiles.value.size > 1) profileManager.setMultiProfileEnabled(true)
        }
    }
}
