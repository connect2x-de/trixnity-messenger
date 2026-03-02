package de.connect2x.trixnity.messenger.i18n

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import web.dom.document

interface AppLanguageUpdater : Worker

private val log = Logger("de.connect2x.trixnity.messenger.i18n.AppLanguageUpdaterImpl")

class AppLanguageUpdaterImpl(private val settings: MatrixMessengerSettingsHolder, private val i18n: I18n) :
    AppLanguageUpdater {
    override suspend fun doWork() =
        settings.map { it.base.preferredLang ?: i18n.currentLang.code }
            .distinctUntilChanged()
            .collect {
                log.debug { "changing lang tag to $it" }
                // TODO the html language tag takes a BCP-47 language code but it is not specified that
                //   Language.code is actually in this format (it works fine for 'de' and 'en')
                // https://developer.mozilla.org/en-US/docs/Glossary/BCP_47_language_tag
                document.documentElement.setAttribute("lang", it.lowercase())
            }
}
