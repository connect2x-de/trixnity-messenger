package de.connect2x.trixnity.messenger.util

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.SimpleInlineTagProvider
import org.intellij.markdown.html.TrimmingInlineHolderProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.parser.LinkMap
import kotlin.collections.plus

interface MatrixMarkdownFlavour : MarkdownFlavourDescriptor

class MatrixMarkdownFlavourImpl : MatrixMarkdownFlavour, GFMFlavourDescriptor() {
    override fun createHtmlGeneratingProviders(
        linkMap: LinkMap,
        baseURI: URI?
    ): Map<IElementType, GeneratingProvider> {
        val map = super.createHtmlGeneratingProviders(linkMap, baseURI).filterKeys { key -> key.name != MarkdownTokenTypes.HARD_LINE_BREAK.name }
            .plus(hashMapOf(
                GFMElementTypes.STRIKETHROUGH to object : SimpleInlineTagProvider("del", 2, -2) {},
                MarkdownTokenTypes.EOL to object : GeneratingProvider {
                    override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
                        if(node.parent?.type == MarkdownElementTypes.PARAGRAPH){
                            visitor.consumeHtml("\n")
                            visitor.consumeHtml("<br />")
                        }
                    }
                }))
        return map
    }
}

