package de.connect2x.trixnity.messenger.viewmodel.uia

import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.model.UserId

class UIAResponse(private var responseStep: net.folivo.trixnity.clientserverapi.client.UIA.Step<*>?) {
    private var responseError: net.folivo.trixnity.clientserverapi.client.UIA.Error<*>? = null

    constructor(responseError: net.folivo.trixnity.clientserverapi.client.UIA.Error<*>) : this(null) {
        this.responseError = responseError
    }

    suspend fun authenticateWithPassword(userId: UserId, password: String): UIAReaction {
        val passwordRequest = AuthenticationRequest.Password(
            IdentifierType.User(userId.full),
            password
        )
        responseStep?.let {
            return UIA.reactToResponse(it.authenticate(passwordRequest))
        }
        responseError?.let {
            return UIA.reactToResponse(it.authenticate(passwordRequest))
        }
        return UIAReaction.DoNothing
    }
}