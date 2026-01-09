package de.connect2x.messenger.compose.view.profiles

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Wizard
import de.connect2x.messenger.compose.view.common.WizardNavigationButton
import de.connect2x.messenger.compose.view.common.WizardStep
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.multi.ProfileManager
import kotlinx.coroutines.launch

@Composable
fun Introduction(open: MutableState<Boolean>) = DI.get<IntroductionView>().create(open)

interface IntroductionView {
    @Composable
    fun create(open: MutableState<Boolean>)
}

class IntroductionViewImpl : IntroductionView {
    @Composable
    override fun create(open: MutableState<Boolean>) {
        val i18n = DI.get<I18nView>()
        val profileManager = DI.get<ProfileManager>()
        var isMultiProfile by remember { mutableStateOf(profileManager.isMultiProfileEnabled.value.let { it != null && it }) }
        val scope = rememberCoroutineScope()

        Wizard(
            listOf(
                WizardStep(
                    id = "de.connect2x.messenger.compose.view.connecting.aa",
                    title = { i18n.profileSelectionMultipleAccountHeader() },
                    content = {
                        ThemedListItemSwitch(
                            headlineContent = { Text(i18n.profileSelectionMultipleAccountSwitch()) },
                            selected = isMultiProfile,
                            onChange = { isMultiProfile = it },
                        )
                    },
                    nextButton = {
                        WizardNavigationButton.Custom {
                            ThemedButton(
                                onClick = {
                                    scope.launch { profileManager.setMultiProfileEnabled(isMultiProfile) }
                                    open.value = false
                                },
                                content = { Text(i18n.commonNext()) },
                                style = MaterialTheme.components.primaryButton
                            )
                        }
                    },
                )
            )
        )
    }
}
