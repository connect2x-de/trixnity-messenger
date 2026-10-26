package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.util.InformationMarkdownFlavour
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication

class MultiMessengerDefaultModulesTest {

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `the root scope resolves what Imprint and Privacy read from it`() {
        val koin = koinApplication { modules(createTrixnityMultiMessengerDefaultModuleFactories().map { it() }) }.koin

        assertNotNull(koin.getOrNull<InformationMarkdownFlavour>())
    }
}
