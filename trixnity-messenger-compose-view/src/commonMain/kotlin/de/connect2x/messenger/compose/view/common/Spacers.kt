package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.theme.messengerDpConstants

@Composable
fun VerySmallSpacer() = Spacer(Modifier.size(MaterialTheme.messengerDpConstants.verySmall))

@Composable
fun SmallSpacer() = Spacer(Modifier.size(MaterialTheme.messengerDpConstants.small))

@Composable
fun MiddleSpacer() = Spacer(Modifier.size(MaterialTheme.messengerDpConstants.middle))

@Composable
fun LargeSpacer() = Spacer(Modifier.size(MaterialTheme.messengerDpConstants.large))
