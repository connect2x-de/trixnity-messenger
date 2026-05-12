package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration.CryptoDriver.LIBOLM
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration.CryptoDriver.VODOZEMAC
import de.connect2x.trixnity.client.CryptoDriverModule
import de.connect2x.trixnity.client.cryptodriver.libolm.libOlm
import de.connect2x.trixnity.client.cryptodriver.vodozemac.vodozemac
import org.koin.dsl.module

fun interface CreateCryptoDriverModule {
    suspend operator fun invoke(): CryptoDriverModule
}

class CreateCryptoDriverModuleImpl(private val config: MatrixMessengerConfiguration) : CreateCryptoDriverModule {
    override suspend fun invoke(): CryptoDriverModule =
        when (config.cryptoDriver) {
            LIBOLM -> CryptoDriverModule.libOlm()
            VODOZEMAC -> CryptoDriverModule.vodozemac()
        }
}

fun createCryptoDriverModuleModule() = module {
    single<CreateCryptoDriverModule> { CreateCryptoDriverModuleImpl(get()) }
}
