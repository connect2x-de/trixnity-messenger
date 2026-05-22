package de.connect2x.trixnity.messenger.viewmodel.util

fun String.afterNewline() = this.substringAfter("\r\n").substringAfter("\n").substringAfter("\r")
