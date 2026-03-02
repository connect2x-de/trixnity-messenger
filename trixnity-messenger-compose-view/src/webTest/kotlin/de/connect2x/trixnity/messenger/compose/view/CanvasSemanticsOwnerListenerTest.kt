@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import js.array.asList
import js.core.JsPrimitives.toKotlinString
import kotlinx.coroutines.delay
import web.dom.Element
import web.dom.ElementId
import web.dom.document
import web.html.HtmlTagName
import web.html.asStringOrNull
import kotlin.contracts.contract
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@ExperimentalMaterial3Api
class CanvasSemanticsOwnerListenerTest {
    // TODO maybe create a dsl like this to define variables for use in both ui and assertions
//    @Test
//    fun aaa() = semTest2 {
//        val tag = "t-btn"
//        ui {
//            Button(
//                onClick = {},
//                modifier = Modifier.semantics { testTag = tag },
//                content = { Text("aaa") },
//            )
//        }
//
//        val btn = a11yRoot.byTestTag(tag) as HTMLButtonElement
//    }

    @Test
    fun `correct button element and inner text`() = a11yTest({
        Button(
            onClick = {},
            modifier = Modifier.semantics { testTag = "t-btn" },
            content = { Text("aaa") },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-btn"),
            tag = "button",
            innerHTML = "aaa",
        )
    }

    @Test
    fun `collection containing only RadioButtons is a radiogroup`() = a11yTest({
        Column(Modifier.semantics {
            testTag = "t-group"
            collectionInfo = CollectionInfo(3, 1)
        }) {
            RadioButton(selected = true, onClick = {})
            RadioButton(selected = false, onClick = {})
            RadioButton(selected = false, onClick = {})
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-group"),
            attrs = mapOf("role" to "radiogroup"),
        )
    }

    @Test
    fun `editable dropdown is text input`() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "role" to "combobox",
                "aria-expanded" to "false"
            ),
        )
    }

    @Test
    fun `readonly dropdown is button`() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "button",
            attrs = mapOf(
                "role" to "combobox",
                "aria-expanded" to "false"
            ),
        )
    }

    @Test
    fun `expanded dropdown is expanded`() = a11yTest({
        var expanded by remember { mutableStateOf(true) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                state = rememberTextFieldState(),
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .semantics {
                        testTag = "dd"
                        if (expanded)
                            collapse { expanded = false; true }
                        else
                            expand { expanded = true; true }
                    },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dd"),
            tag = "button",
            attrs = mapOf(
                "role" to "combobox",
                "aria-expanded" to "true"
            ),
        )
    }

    @Test
    fun `checkbox is input type checkbox`() = a11yTest({
        Checkbox(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "cb" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("cb"),
            tag = "input",
            attrs = mapOf(
                "type" to "checkbox",
            ),
        )
    }

    @Test
    fun `checked checkbox is checked`() = a11yTest({
        Checkbox(
            checked = true,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "cb" },
        )
    }) { a11yRoot ->
        val cb = a11yRoot.byTestTag("cb")
        assertElem(cb, "input", mapOf("type" to "checkbox"))
        assertTrue(cb.checked, "checkbox not checked")
    }

    @Test
    fun `switch is button role switch`() = a11yTest({
        Switch(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "switch" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "false",
            ),
        )
    }

    @Test
    fun `checked switch is checked`() = a11yTest({
        Switch(
            checked = true,
            onCheckedChange = {},
            modifier = Modifier.semantics { testTag = "switch" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "true",
            ),
        )
    }

    @Test
    fun `semantics text switch is aria label`() = a11yTest({
        Switch(
            checked = false,
            onCheckedChange = {},
            modifier = Modifier.semantics {
                text = AnnotatedString("lorem")
                testTag = "switch"
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("switch"),
            tag = "button",
            attrs = mapOf(
                "role" to "switch",
                "aria-checked" to "false",
                "aria-label" to "lorem"
            ),
        )
    }

    @Test
    fun `radio button is input type radio`() = a11yTest({
        RadioButton(
            selected = false,
            onClick = {},
            modifier = Modifier.semantics { testTag = "rbtn" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("rbtn"),
            tag = "input",
            attrs = mapOf("type" to "radio"),
        )
    }

    @Test
    fun `checked radio button is checked`() = a11yTest({
        RadioButton(
            selected = true,
            onClick = {},
            modifier = Modifier.semantics { testTag = "rbtn" },
        )
    }) { a11yRoot ->
        val rb = a11yRoot.byTestTag("rbtn")
        assertElem(
            elem = rb,
            tag = "input",
            attrs = mapOf("type" to "radio"),
        )
        assertTrue(rb.checked, "radio button not checked")
    }

    @Test
    fun `radio button semantics text is aria label`() = a11yTest({
        RadioButton(
            selected = false,
            onClick = {},
            modifier = Modifier.semantics {
                text = androidx.compose.ui.text.AnnotatedString("rbtn text")
                testTag = "rbtn"
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("rbtn"),
            tag = "input",
            attrs = mapOf("type" to "radio", "aria-label" to "rbtn text"),
            innerHTML = "",
        )
    }

    @Test
    fun `text field is input type text`() = a11yTest({
        TextField(
            state = rememberTextFieldState("lorem"),
            modifier = Modifier.semantics { testTag = "tf" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tf"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "aria-description" to "lorem",
            ),
            innerHTML = "",
        )
    }

    @Test
    fun `readonly text field readonly`() = a11yTest({
        TextField(
            state = rememberTextFieldState("lorem"),
            readOnly = true,
            modifier = Modifier.semantics { testTag = "tf" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tf"),
            tag = "input",
            attrs = mapOf(
                "type" to "text",
                "aria-description" to "lorem",
                "readonly" to "",
            ),
            innerHTML = "",
        )
    }

    @Test
    fun `dialog has role dialog`() = a11yTest({
        Dialog(onDismissRequest = {}) {
            Box(modifier = Modifier.semantics(true) { testTag = "dg" }) {
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("dg")?.parentElement,
            tag = "div",
            attrs = mapOf("role" to "dialog"),
        )
    }

    @Test
    fun `progress bar is progress`() = a11yTest({
        LinearProgressIndicator(
            progress = { 0.5F },
            modifier = Modifier.semantics { testTag = "pb" },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
            ),
            attrAsserter = ProgressAttrAsserter
        )
    }

    @Test
    fun `progress bar range info is progress`() = a11yTest({
        Box(
            modifier = Modifier.semantics {
                testTag = "pb"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = 0.5f,
                    range = 0f..1f,
                )
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
            ),
            attrAsserter = ProgressAttrAsserter
        )
    }

    @Test
    fun `labeled progress bar is has label`() = a11yTest({
        LinearProgressIndicator(
            progress = { 0.5F },
            modifier = Modifier.semantics {
                testTag = "pb"
                text = androidx.compose.ui.text.AnnotatedString("lorem ipsum")
            },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("pb"),
            tag = "progress",
            attrs = mapOf(
                "value" to "0.5",
                "max" to "1",
                "aria-label" to "lorem ipsum",
            ),
            attrAsserter = { attrs, actualAttrs ->
                ProgressAttrAsserter(attrs, actualAttrs)
                assertEquals(attrs?.get("aria-label"), actualAttrs?.get("aria-label"))
            }
        )
    }

    @Test
    fun `live region is aria live`() = a11yTest({
        Text(
            text = "Lorem ipsum",
            modifier = Modifier.semantics {
                testTag = "t"
                liveRegion = LiveRegionMode.Assertive
            },
        )
    }) { a11yRoot ->
        assertElem(
            a11yRoot.byTestTag("t"),
            tag = "div",
            attrs = mapOf(
                "aria-live" to "assertive",
                "aria-label" to "Lorem ipsum",
            ),
        )
    }

    @Test
    fun `tooltip is role tooltip`() = a11yTest({
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above, 4.dp),
            state = rememberTooltipState(),
            modifier = Modifier.semantics {
                testTag = "tt"
                paneTitle = "tooltip"
            },
            tooltip = { Text("tooltiptext") },
        ) {
            Button(
                onClick = {},
                content = { Text("btn text") },
            )
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tt"),
            tag = "div",
            attrs = mapOf(
                "role" to "tooltip",
                "title" to "tooltip",
            ),
        )
    }

    @Test
    fun `button opening popup menu is expandable `() = a11yTest({
        var expanded by remember { mutableStateOf(false) }
        Button(
            onClick = { expanded != expanded },
            modifier = Modifier.semantics {
                testTag = "btn"
                if (expanded) collapse { expanded = false; true }
                else expand { expanded = true; true }
            },
        ) {
            Text("Open me")
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem({ Text("a") }, {})
                DropdownMenuItem({ Text("b") }, {})
                DropdownMenuItem({ Text("c") }, {})
            }
        }
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("btn"),
            tag = "button",
            attrs = mapOf("aria-expanded" to "false"),
            innerHTML = "Open me",
        )
    }

    @Test
    fun `dialog with heading is labelled by`() = a11yTest({
        Dialog(onDismissRequest = {}) {
            Text("lorem", Modifier.semantics {
                testTag = "lbl"
                heading()
            })
        }
    }) { a11yRoot ->
        val lbl = a11yRoot.byTestTag("lbl")!!
        assertElem(
            elem = lbl,
            tag = "div",
            attrs = mapOf("aria-label" to "lorem"),
        )
        assertElem(
            elem = lbl.parentElement,
            tag = "div",
            attrs = mapOf("role" to "dialog", "aria-labelledby" to lbl.id.value),
        )
    }

    @Test
    fun `dialog with heading and content is labelled by and described by`() = a11yTest({
        Dialog(onDismissRequest = {}) {
            Text("lorem", Modifier.semantics { heading(); testTag = "lbl" })
            Text("ipsum", Modifier.semantics { testTag = "inner" })
            Text("dolor", Modifier.semantics { testTag = "inner2" })
        }
    }) { a11yRoot ->
        val lbl = a11yRoot.byTestTag("lbl")!!
        assertElem(
            elem = lbl,
            tag = "div",
            attrs = mapOf("aria-label" to "lorem"),
        )
        val inner = a11yRoot.byTestTag("inner")!!
        assertElem(
            elem = lbl.parentElement,
            tag = "div",
            attrs = mapOf("role" to "dialog", "aria-labelledby" to lbl.id.value, "aria-describedby" to inner.id.value),
        )
    }

    @Test
    fun `button with content description`() = a11yTest({
        Button(
            onClick = {},
            modifier = Modifier.semantics {
                testTag = "t-btn"
                contentDescription = "some content"
            },
            content = { Text("aaa") },
        )
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-btn"),
            attrs = mapOf("aria-description" to "some content"),
            tag = "button",
            innerHTML = "aaa",
        )
    }

    @Test
    fun `box with content description and label`() = a11yTest({
        Box(modifier = Modifier.semantics {
            testTag = "t-box"
            text = AnnotatedString("some text")
            contentDescription = "some content"
        })
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("t-box"),
            attrs = mapOf(
                "aria-label" to "some text",
                "aria-description" to "some content"
            ),
            tag = "div",
        )
    }

    @Test
    fun `text with content description`() = a11yTest({
        Text("some text", modifier = Modifier.semantics {
            testTag = "tt"
            contentDescription = "some content"
        })
    }) { a11yRoot ->
        assertElem(
            elem = a11yRoot.byTestTag("tt"),
            attrs = mapOf(
                "aria-label" to "some text",
                "aria-description" to "some content"
            ),
            tag = "div",
        )
    }
}

private typealias AttrAsserter = (attrs: Map<String, String>?, actualAttrs: Map<String, String>?) -> Unit

private val DefaultAttrAsserter: AttrAsserter = { attrs, actualAttrs ->
    assertEquals(attrs, actualAttrs, "wrong attributes")
}

private val ProgressAttrAsserter: AttrAsserter = { attrs, actualAttrs ->
    assertNotNull(attrs)
    assertNotNull(actualAttrs)

    assertEquals(attrs["value"]?.toFloat(),  actualAttrs["value"]?.toFloat())
    assertEquals(attrs["max"]?.toFloat(), actualAttrs["max"]?.toFloat())
}

@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class, InternalTestApi::class)
private fun a11yTest(content: @Composable () -> Unit, assertions: suspend (Element) -> Unit) : Promise<JsAny?> {
    val a11yRoot = document.createElement(HtmlTagName.div)
    document.body.appendChild(a11yRoot)
    return SkikoComposeUiTest(semanticsOwnerListener = CanvasSemanticsOwnerListener(a11yRoot)).runTest {
        setContent(content)

        delay(10.seconds)// This is virtual time

        println(a11yRoot.innerHTML)
        assertions(a11yRoot)

        document.body.removeChild(a11yRoot)
    }
}

private fun Element.byTestTag(tag: String): Element? = this.querySelector("[data-test-tag='$tag']")

private fun assertAttrs(el: Element?, attrs: Map<String, String>?, attrAsserter: AttrAsserter) {
    val actualAttrs = el?.attributes?.asList()?.associate { Pair(it.name, it.value) }?.toMutableMap()
    for (key in listOf("id", "style", "data-test-tag"))
        actualAttrs?.remove(key)
    attrAsserter(attrs, actualAttrs)
}

private fun assertElem(
    elem: Element?,
    tag: String? = null,
    attrs: Map<String, String>? = null,
    innerHTML: String? = null,
    attrAsserter: AttrAsserter = DefaultAttrAsserter,
) {
    contract { returns() implies (elem != null) }

    assertNotNull(elem)
    if (tag != null)
        assertEquals(tag.lowercase(), elem.tagName.lowercase(), "wrong tag")
    if (attrs != null)
        assertAttrs(elem, attrs, attrAsserter)
    if (innerHTML != null)
        assertEquals(innerHTML, elem.innerHTML.asStringOrNull(), "wrong inner html")
}

private external interface ElementWithChecked : JsAny {
    val checked: Boolean?
}

private val Element.checked: Boolean
    get() = unsafeCast<ElementWithChecked>().checked == true

private val ElementId.value: String
    get() = unsafeCast<JsString>().toKotlinString()
