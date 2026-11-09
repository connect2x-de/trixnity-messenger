@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.livekitnity.compose.ui

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement

/**
 * CMP-version-specific HTML underlay adapter extracted from the supplied view. Changes the whole
 * viewport's interop stacking, so use only in a video-only interop viewport. Do not put this under
 * an offscreen compositing layer, saveLayer-producing alpha animation, or RenderEffect: Clear must
 * reach the main canvas. Callers own clipping, backgrounds, borders, and overlays. The remembered
 * element must not be reused through HtmlElementView.onReset.
 */
internal fun Modifier.htmlUnderlay(element: HTMLElement): Modifier =
    this then HtmlUnderlayElement(element)

private data class HtmlUnderlayElement(
    val element: HTMLElement,
) : ModifierNodeElement<HtmlUnderlayNode>() {

  override fun create(): HtmlUnderlayNode = HtmlUnderlayNode(element)

  override fun update(node: HtmlUnderlayNode) {
    node.updateElement(element)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "htmlUnderlay"
    properties["element"] = element
  }
}

private class HtmlUnderlayNode(
    private var element: HTMLElement,
) : Modifier.Node(), DrawModifierNode {

  private var job: Job? = null
  private var ready: Boolean = false

  override fun onAttach() {
    super.onAttach()
    startAcquisition()
  }

  override fun onDetach() {
    job?.cancel()
    job = null
    ready = false
    super.onDetach()
  }

  fun updateElement(newElement: HTMLElement) {
    if (element != newElement) {
      element = newElement
      if (isAttached) {
        startAcquisition()
      }
    }
  }

  private fun startAcquisition() {
    job?.cancel()
    ready = false
    invalidateDraw()

    job = coroutineScope.launch {
      var lease: JsAny? = null
      try {
        var attempts = 0
        while (lease == null && attempts < 120) {
          withFrameNanos {}
          lease = acquireVideoUnderlay(element)
          attempts++
        }
        check(lease != null) {
          "CMP video underlay: expected canvas/shadow-host/interop sibling structure was not found. " +
              "This CMP version needs a different DOM adapter."
        }
        ready = true
        invalidateDraw()
        awaitCancellation()
      } finally {
        ready = false
        invalidateDraw()
        lease?.let { releaseVideoUnderlay(it) }
      }
    }
  }

  override fun ContentDrawScope.draw() {
    drawContent()
    if (ready) {
      drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
    }
  }
}

// Private DOM hack. Locate the structure from this particular video, not from
// document.querySelector("canvas"), which could select another viewport.
// The styles are reference-counted because multiple tiles share one viewport.
private fun acquireVideoUnderlay(video: HTMLElement): JsAny? =
    js(
        """
    (() => {
        if (!video.isConnected) return null;
        let branch = video;
        while (branch.parentElement) {
            const root = branch.parentElement;
            const shadowHost = Array.from(root.children).find(child =>
                child !== branch && child.shadowRoot &&
                child.shadowRoot.querySelector('canvas')
            );
            if (shadowHost) {
                const key = Symbol.for('kall.cmp.video-underlay.v2');
                let record = root[key];
                if (!record) {
                    const saved = [];
                    function set(element, property, value) {
                        saved.push({element, property,
                            value: element.style.getPropertyValue(property),
                            priority: element.style.getPropertyPriority(property)});
                        element.style.setProperty(property, value);
                    }
                    // Both children are positioned by CMP. z-index establishes
                    // the ordering without moving nodes owned by its runtime.
                    set(shadowHost, 'z-index', '2');
                    set(branch, 'z-index', '1');
                    const canvas = shadowHost.shadowRoot.querySelector('canvas');
                    record = {root, key, shadowHost, canvas, interop: branch,
                        saved, refs: 0, videos: new Map()};
                    // HTML interop wrappers can catch wheel input even when the
                    // video itself has pointer-events:none. Route only those
                    // events to CMP's existing canvas wheel handler.
                    record.wheel = event => {
                        const path = event.composedPath();
                        if (path.includes(canvas)) return; // Already handled by Compose.
                        if (event.ctrlKey || event.metaKey) return; // Preserve browser zoom.
                        if (!path.includes(record.interop)) return;
                        const overVideo = Array.from(record.videos.keys()).some(element => {
                            if (!element.isConnected) return false;
                            const r = element.getBoundingClientRect();
                            return event.clientX >= r.left && event.clientX < r.right &&
                                   event.clientY >= r.top && event.clientY < r.bottom;
                        });
                        if (!overVideo) return;
                        const forwarded = new WheelEvent('wheel', {
                            bubbles: true, cancelable: true, composed: true,
                            view: window,
                            clientX: event.clientX, clientY: event.clientY,
                            screenX: event.screenX, screenY: event.screenY,
                            deltaX: event.deltaX, deltaY: event.deltaY,
                            deltaZ: event.deltaZ, deltaMode: event.deltaMode,
                            shiftKey: event.shiftKey, ctrlKey: event.ctrlKey,
                            altKey: event.altKey, metaKey: event.metaKey,
                            button: event.button, buttons: event.buttons,
                        });
                        // CMP reads offsetX/Y and multiplies by its density.
                        // Supply canvas-local CSS pixels, not device pixels or
                        // offsets relative to the original HTML video element.
                        const canvasBounds = canvas.getBoundingClientRect();
                        Object.defineProperties(forwarded, {
                            offsetX: {value: event.clientX - canvasBounds.left},
                            offsetY: {value: event.clientY - canvasBounds.top},
                        });
                        canvas.dispatchEvent(forwarded);
                        // Prevent a second handler receiving the original wheel.
                        event.stopImmediatePropagation();
                        // If CMP did not consume it, preserve native page scrolling.
                        if (forwarded.defaultPrevented && event.cancelable) event.preventDefault();
                    };
                    root.addEventListener('wheel', record.wheel, {capture: true, passive: false});
                    root[key] = record;
                }
                if (record.shadowHost !== shadowHost || record.interop !== branch) {
                    throw new Error('CMP video underlay: unexpected shared DOM structure');
                }
                record.refs++;
                record.videos.set(video, (record.videos.get(video) || 0) + 1);
                return {record, video, released: false};
            }
            branch = root;
        }
        return null;
    })()
""",
    )

private fun releaseVideoUnderlay(lease: JsAny): Unit =
    js(
        """
    {
        if (!lease.released) {
            lease.released = true;
            const record = lease.record;
            record.refs--;
            const remaining = (record.videos.get(lease.video) || 1) - 1;
            if (remaining > 0) record.videos.set(lease.video, remaining);
            else record.videos.delete(lease.video);
            if (record.refs === 0) {
                record.root.removeEventListener('wheel', record.wheel, true);
                for (const old of record.saved) {
                    if (old.value) old.element.style.setProperty(old.property, old.value, old.priority);
                    else old.element.style.removeProperty(old.property);
                }
                delete record.root[record.key];
            }
        }
    }
""",
    )
