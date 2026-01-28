package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.ErrorView
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogContent
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogFooter
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.messenger.compose.view.util.inputFocusNavigation
import de.connect2x.trixnity.messenger.util.isKnock
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModel.SearchGroup

interface SearchGroupView {
    @Composable
    fun create(searchGroupViewModel: SearchGroupViewModel)
}

@Composable
fun SearchGroup(searchGroupViewModel: SearchGroupViewModel) {
    DI.get<SearchGroupView>().create(searchGroupViewModel)
}

class SearchGroupViewImpl : SearchGroupView {
    @Composable
    override fun create(searchGroupViewModel: SearchGroupViewModel) {
        val i18n = DI.get<I18nView>()
        val reason = remember { mutableStateOf(TextFieldValue()) }
        val knockGroupModalShownFor = remember { mutableStateOf<SearchGroup?>(null) }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box {
                Column {
                    Header(searchGroupViewModel::back, i18n.searchGroupTitle())
                    SearchGroupSearchBar(searchGroupViewModel)
                    Spacer(Modifier.size(10.dp))
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                    Spacer(Modifier.size(10.dp))
                    SearchGroupResults(searchGroupViewModel, knockGroupModalShownFor)
                }
            }

            knockGroupModalShownFor.value?.let {
                val onCancel = {
                    knockGroupModalShownFor.value = null
                }
                val i18n = DI.get<I18nView>()
                ThemedAdaptiveDialog(onCancel) {
                    AdaptiveDialogHeader {
                        Text(i18n.knockRequest())
                    }
                    AdaptiveDialogContent {
                        Text(i18n.knockExplanation())
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = reason.value,
                            onValueChange = { reason.value = it },
                            label = { Text(i18n.knockLabel()) }
                        )
                    }
                    AdaptiveDialogFooter {
                        ThemedButton(
                            style = MaterialTheme.components.commonButton,
                            onClick = onCancel
                        ) {
                            Text(i18n.actionCancel())
                        }
                        ThemedButton(
                            style = MaterialTheme.components.primaryButton,
                            onClick = {
                                searchGroupViewModel.enterGroup(it.roomId, reason.value.text)
                                knockGroupModalShownFor.value = null
                            }
                        ) {
                            Text(i18n.actionSubmit())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchGroupSearchBar(searchGroupViewModel: SearchGroupViewModel) {
    var searchTerm by searchGroupViewModel.searchTerm.collectAsTextFieldValueState()

    val i18n = DI.get<I18nView>()

    OutlinedTextField(
        searchTerm,
        { searchTerm = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .inputFocusNavigation(),
        label = { Text(i18n.searchGroupSearch()) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            autoCorrectEnabled = false
        )
    )
}

@Composable
fun SearchGroupResults(
    searchGroupViewModel: SearchGroupViewModel,
    knockGroupModalShownFor: MutableState<SearchGroup?>
) {
    val foundGroups by searchGroupViewModel.foundGroups.collectAsState()
    val groupSearchInProgress = searchGroupViewModel.groupSearchInProgress.collectAsState().value
    val error by searchGroupViewModel.error.collectAsState()
    val listState = rememberLazyListState()

    val i18n = DI.get<I18nView>()

    Column(Modifier.fillMaxSize(), Arrangement.Top) {
        error?.let { ErrorView(it) }

        if (groupSearchInProgress) {
            ThemedProgressIndicator(
                Modifier.fillMaxWidth(),
                MaterialTheme.components.linearProgressIndicator
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                if (foundGroups.isEmpty()) {
                    Text(i18n.searchGroupNotFound())
                } else {
                    var focusedItem by remember(foundGroups) {
                        mutableStateOf(foundGroups.map { it.roomId.full }.firstOrNull())
                    }
                    LazyColumn(Modifier.fillMaxSize().rovingFocusContainer(), listState) {
                        items(foundGroups, { group -> group.roomId.full }) { group ->
                            SearchGroupResult(
                                group = group,
                                modifier = Modifier.rovingFocusItem(
                                    isFocused = focusedItem == group.roomId.full,
                                    onFocus = { focusedItem = group.roomId.full },
                                ),
                                searchGroupViewModel = searchGroupViewModel,
                                knockGroupModalShownFor = knockGroupModalShownFor,
                            )
                        }
                    }
                    if (listState.canScrollForward || listState.canScrollBackward) {
                        VerticalScrollbar(Modifier.align(Alignment.CenterEnd), listState, false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupResult(
    group: SearchGroup,
    modifier: Modifier,
    searchGroupViewModel: SearchGroupViewModel,
    knockGroupModalShownFor: MutableState<SearchGroup?>
) {
    val image = group.image.collectAsState().value

    Tooltip({ Text(group.groupName) }) {
        ThemedListItemButton(
            modifier = modifier,
            leadingContent = { ThemedUserAvatar(group.initials, image) },
            headlineContent = {
                Text(
                    group.groupName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = group.topic?.let {
                {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            onClick = {
                if (group.joinRule.isKnock) {
                    knockGroupModalShownFor.value = group
                } else {
                    searchGroupViewModel.enterGroup(group.roomId)
                }
            },
        )
    }
}
