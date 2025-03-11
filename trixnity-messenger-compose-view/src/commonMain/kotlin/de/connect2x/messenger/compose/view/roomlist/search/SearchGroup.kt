package de.connect2x.messenger.compose.view.roomlist.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.TextFieldModal
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModel

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

        Box(Modifier.fillMaxSize()) {
            Box {
                Column {
                    Header(searchGroupViewModel::back, i18n.searchGroupTitle())
                    SearchGroupSearchBar(searchGroupViewModel)
                    Spacer(Modifier.size(10.dp))
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                    Spacer(Modifier.size(10.dp))
                    SearchGroupResults(searchGroupViewModel)
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
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        label = { Text(i18n.searchGroupSearch()) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            autoCorrectEnabled = false
        )
    )
}

@Composable
fun SearchGroupResults(searchGroupViewModel: SearchGroupViewModel) {
    val foundGroups = searchGroupViewModel.foundGroups.collectAsState().value
    val groupSearchInProgress = searchGroupViewModel.groupSearchInProgress.collectAsState().value
    val listState = rememberLazyListState()

    val i18n = DI.get<I18nView>()

    Box(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (groupSearchInProgress) {
            LoadingSpinner()
        } else {
            if (foundGroups.isEmpty()) {
                Text(i18n.searchGroupNotFound())
            } else {
                LazyColumn(Modifier.fillMaxSize(), listState) {
                    items(foundGroups, { group -> group.roomId.full }) { group ->
                        SearchGroupResult(group, searchGroupViewModel)
                    }
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), listState, false)
            }
        }
    }
}

@Composable
fun SearchGroupResult(group: SearchGroupViewModel.SearchGroup, searchGroupViewModel: SearchGroupViewModel) {
    val i18n = DI.current.get<I18nView>()
    val image = group.image.collectAsState().value
    val knockGroupModalShown = searchGroupViewModel.knockGroupModalShown.collectAsState().value
    val reason = remember { mutableStateOf(TextFieldValue()) }

    Tooltip({ TooltipText(group.groupName) }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    if (group.isKnock) {
                        searchGroupViewModel.showKnockGroupModal()
                    } else {
                        searchGroupViewModel.joinGroup(group)
                    }
                }
                .buttonPointerModifier()
                .padding(bottom = 20.dp)
        ) {
            Avatar(image, group.initials)
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    group.groupName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                group.topic?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (knockGroupModalShown) {
        TextFieldModal(
            title = i18n.knockRequest(),
            description = i18n.knockExplanation(),
            textFieldValueState = reason,
            onSubmit = {
                searchGroupViewModel.knockGroup(group, reason.value.text)
                searchGroupViewModel.hideKnockGroupModal()
            },
            onCancel = {
                searchGroupViewModel.hideKnockGroupModal()
            }
        )
    }
}
