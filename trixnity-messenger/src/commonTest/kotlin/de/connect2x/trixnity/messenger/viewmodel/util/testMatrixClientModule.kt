package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.NamedMatrixClient
import de.connect2x.trixnity.messenger.NamedMatrixClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun testMatrixClientModule(matrixClientMock: MatrixClient, accountName: String = "test"): Module = module {
    single {
        NamedMatrixClients(
            MutableStateFlow(
                listOf(
                    NamedMatrixClient(
                        accountName,
                        MutableStateFlow(matrixClientMock),
                        CoroutineScope(Dispatchers.Default),
                    )
                )
            )
        )
    }

    single {
        object : GetAccountNames {
            override suspend fun invoke(): List<String> {
                return listOf(accountName)
            }
        }
    }
}

fun testMatrixClientModule(
    matrixClientMocks: List<MatrixClient>,
    accountNames: List<String>,
): Module = module {
    single {
        NamedMatrixClients(
            MutableStateFlow(
                matrixClientMocks.zip(accountNames) { matrixClientMock, accountName ->
                    NamedMatrixClient(
                        accountName,
                        MutableStateFlow(matrixClientMock),
                        CoroutineScope(Dispatchers.Default),
                    )
                }
            )
        )
    }

    single {
        object : GetAccountNames {
            override suspend fun invoke(): List<String> {
                return accountNames
            }
        }
    }
}