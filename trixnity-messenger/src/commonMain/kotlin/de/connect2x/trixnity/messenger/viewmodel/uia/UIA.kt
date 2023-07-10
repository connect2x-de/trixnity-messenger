package de.connect2x.trixnity.messenger.viewmodel.uia

import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.ErrorResponse


object UIA {

    fun reactToResponse(response: Result<UIA<*>>): UIAReaction {
        return response.fold(
            onSuccess = {
                when (it) {
                    is UIA.Error<*> -> if (it.errorResponse is ErrorResponse.Forbidden) {
                        UIAReaction.ShowLogin(UIAResponse(it))
                    } else {
                        UIAReaction.UnexpectedError(it.errorResponse.error)
                    }

                    is UIA.Step<*> -> { // TODO add more than password
                        val canUsePassword = it.state.flows.any { flow ->
                            flow.stages.firstOrNull() is AuthenticationType.Password
                        }
                        if (canUsePassword) {
                            UIAReaction.ShowLogin(UIAResponse(it))
                        } else {
                            UIAReaction.DoNothing
                        }
                    }

                    else -> UIAReaction.DoNothing
                }
            },
            onFailure = {
                UIAReaction.UnexpectedError(error = it.message)
            }
        )
    }

}