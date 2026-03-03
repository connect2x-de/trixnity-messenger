package de.connect2x.trixnity.messenger.util

import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor


interface InformationMarkdownFlavour : MarkdownFlavourDescriptor

class InformationMarkdownFlavourImpl : InformationMarkdownFlavour, GFMFlavourDescriptor()
