package de.connect2x.trixnity.messenger.compose.view.roomlist.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.roomlist.search.SearchUsersView
import de.connect2x.trixnity.messenger.compose.view.search.SearchResultState
import de.connect2x.trixnity.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.trixnity.messenger.compose.view.search.collectUserSearchResult
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarContentIcon
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedAvatar
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.util.LocalRovingFocus
import de.connect2x.trixnity.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.util.verticalRovingFocus
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

interface CreateNewChatView {
    @Composable
    fun create(createNewChatViewModel: CreateNewChatViewModel)
}

@Composable
fun CreateNewChat(createNewChatViewModel: CreateNewChatViewModel) {
    DI.get<CreateNewChatView>().create(createNewChatViewModel)
}

class CreateNewChatViewImpl : CreateNewChatView {
    @Composable
    override fun create(createNewChatViewModel: CreateNewChatViewModel) {
        val i18n = DI.get<I18nView>()
        val isCreating by createNewChatViewModel.isCreating.collectAsState()
        val error = createNewChatViewModel.error.collectAsState().value
        val errorDetails = createNewChatViewModel.errorDetails.collectAsState().value
        val searchUsersView = DI.get<SearchUsersView>()
        val userSearchResultView = DI.get<UserSearchResultListView>()
        val searchUsersResults = collectUserSearchResult(createNewChatViewModel.createNewRoomViewModel.searchHandler)
        val listState = rememberLazyListState()
        val references = remember(searchUsersResults) {
            (searchUsersResults as? SearchResultState.Results)?.users?.map { it.userId.full }
        }
        val defaultItem = references?.firstOrNull()

        Column(Modifier.fillMaxSize()) {
            Header(createNewChatViewModel::cancel, i18n.createNewChatTitle())
            Box(Modifier.fillMaxSize()) {
                RovingFocusContainer {
                    val focusContainer = LocalRovingFocus.current
                    val currentRef = focusContainer?.activeRef?.value
                    LaunchedEffect(currentRef) {
                        if (currentRef != null) {
                            if (references?.contains(currentRef) != true) {
                                focusContainer.activeRef.value = null
                            }
                        }
                    }

                    val focusModifier = if (references != null) Modifier.Companion.verticalRovingFocus(
                        default = defaultItem,
                        scroll = { item ->
                            val index = references.indexOf(item)
                            if (index != -1) {
                                listState.scrollToItem(index)
                            }
                        },
                        up = {
                            val currentItem = activeRef.value ?: defaultItem
                            val currentIndex = references.indexOf(currentItem)
                            val nextIndex = currentIndex.minus(1).coerceIn(references.indices)
                            references[nextIndex]
                        },
                        down = {
                            val currentItem = activeRef.value ?: defaultItem
                            val currentIndex = references.indexOf(currentItem)
                            val nextIndex = currentIndex.plus(1).coerceIn(references.indices)
                            references[nextIndex]
                        },
                    ) else Modifier

                    LazyColumn(
                        modifier = Modifier.then(focusModifier),
                        state = listState,
                    ) {
                        item(key = "CreatingIndicator") {
                            if (isCreating) {
                                ThemedProgressIndicator(
                                    Modifier.fillMaxWidth(),
                                    MaterialTheme.components.linearProgressIndicator
                                )
                            }
                        }
                        item(key = "AddOrSearchGroup") {
                            AddOrSearchGroup(createNewChatViewModel)
                            HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
                        }
                        searchUsersView.create(
                            createNewChatViewModel.createNewRoomViewModel,
                            createNewChatViewModel::onUserClick,
                            searchUsersResults,
                            userSearchResultView,
                            this,
                        )
                    }
                }

                if (listState.canScrollForward || listState.canScrollBackward) {
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        lazyListState = listState,
                        reverseLayout = false,
                    )
                }
            }
        }

        if (error != null) {
            ThemedModalDialog({ createNewChatViewModel.errorDismiss() }) {
                ModalDialogHeader {
                    Text(i18n.anErrorHasOccurred())
                }
                ModalDialogContent {
                    Text(error)
                    if (errorDetails != null) {
                        ExpandableSection(heading = i18n.errorDetails(), icon = Icons.Default.Info) {
                            Text(errorDetails, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { createNewChatViewModel.errorDismiss() },
                    ) {
                        Text(i18n.actionOk())
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrSearchGroup(createNewChatViewModel: CreateNewChatViewModel) {
    val i18n = DI.get<I18nView>()
    val isCreating by createNewChatViewModel.isCreating.collectAsState()
    val interactionSourceCreateGroup = remember { MutableInteractionSource() }
    val interactionSourceSearchGroup = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1.0f, fill = true)
                .clickable(
                    enabled = !isCreating,
                    interactionSource = interactionSourceCreateGroup,
                    indication = null
                ) { createNewChatViewModel.createGroup() }
                .focusHighlighting(interactionSource = interactionSourceCreateGroup)
                .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedAvatar(avatarSize().dp) {
                    AvatarContentIcon(Icons.Default.GroupAdd, avatarSize().dp)
                }
                Spacer(Modifier.size(20.dp))
                Text(i18n.createNewGroupCreate())
            }
        }
        Spacer(Modifier.size(20.dp))
        Box(
            Modifier.weight(1.0f, fill = true)
                .clickable(
                    interactionSource = interactionSourceSearchGroup,
                    indication = null
                ) { createNewChatViewModel.searchGroup() }
                .focusHighlighting(interactionSource = interactionSourceSearchGroup)
                .buttonPointerModifier()
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedAvatar(avatarSize().dp) {
                    AvatarContentIcon(Icons.Default.TravelExplore, avatarSize().dp)
                }
                Spacer(Modifier.size(20.dp))
                Text(i18n.createNewGroupSearch())
            }
        }
    }
}
