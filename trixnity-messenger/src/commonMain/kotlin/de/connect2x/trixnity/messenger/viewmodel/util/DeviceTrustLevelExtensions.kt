package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.crypto.key.DeviceTrustLevel

val DeviceTrustLevel.isVerified: Boolean
    get() = this is DeviceTrustLevel.CrossSigned && this.verified
            || this is DeviceTrustLevel.Valid && this.verified
