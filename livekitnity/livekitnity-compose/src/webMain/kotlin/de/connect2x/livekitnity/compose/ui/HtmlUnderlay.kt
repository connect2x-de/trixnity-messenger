@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.livekitnity.compose.ui

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.toSvg
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event

/**
 * CMP-version-specific HTML underlay adapter extracted from the supplied view. Changes the whole
 * viewport's interop stacking, so use only in a video-only interop viewport. Do not put this under
 * an offscreen compositing layer, saveLayer-producing alpha animation, or RenderEffect: Clear must
 * reach the main canvas. The supplied shape clips the HTML video. Callers own canvas clipping,
 * backgrounds, borders, and overlays. The remembered element must not be reused through
 * HtmlElementView.onReset.
 */
internal fun Modifier.htmlUnderlay(element: HTMLVideoElement, shape: Shape): Modifier {
  return this then HtmlUnderlayElement(element, shape)
}

private data class HtmlUnderlayElement(
    val element: HTMLVideoElement,
    val shape: Shape,
) : ModifierNodeElement<HtmlUnderlayNode>() {

  override fun create(): HtmlUnderlayNode {
    return HtmlUnderlayNode(element, shape)
  }

  override fun update(node: HtmlUnderlayNode) {
    node.updateElement(element)
    node.updateShape(shape)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "htmlUnderlay"
    properties["element"] = element
  }
}

private class HtmlUnderlayNode(
    private var element: HTMLVideoElement,
    private var shape: Shape,
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

  fun updateElement(newElement: HTMLVideoElement) {
    if (element != newElement) {
      element = newElement
      if (isAttached) {
        startAcquisition()
      }
    }
  }

  fun updateShape(newShape: Shape) {
    shape = newShape
  }

  private fun startAcquisition() {
    job?.cancel()
    ready = false
    invalidateDraw()

    job = coroutineScope.launch {
      val video = element
      val invalidate: (Event) -> Unit = { invalidateDraw() }
      video.addEventListener("loadeddata", invalidate)
      video.addEventListener("emptied", invalidate)
      var lease: JsAny? = null
      try {
        var attempts = 0
        while (lease == null && attempts < 120) {
          withFrameNanos {}
          lease = acquireVideoUnderlay(video)
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
        video.removeEventListener("loadeddata", invalidate)
        video.removeEventListener("emptied", invalidate)
        ready = false
        invalidateDraw()
        lease?.let { releaseVideoUnderlay(it) }
      }
    }
  }

  override fun ContentDrawScope.draw() {
    drawContent()
    if (ready) {
      element.style.width = "${size.width / density}px"
      element.style.height = "${size.height / density}px"
      val outline = shape.createOutline(size, layoutDirection, this)
      element.style.setProperty("clip-path", outline.toCssClipPath(size, density))
      val hasDecodedFrame = element.readyState >= VideoReadyStateHaveCurrentData
      element.style.setProperty("visibility", if (hasDecodedFrame) "visible" else "hidden")
      if (hasDecodedFrame && size.minDimension > 2 * VideoCanvasInsetDevicePixels) {
        inset(VideoCanvasInsetDevicePixels) {
          drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
        }
      }
    }
  }
}

private const val VideoReadyStateHaveCurrentData = 2
private const val VideoClipOverlapDevicePixels = 1f
private const val VideoCanvasInsetDevicePixels = 1f

private fun Outline.toCssClipPath(size: Size, density: Float): String {
  return when (this) {
    is Outline.Rectangle -> toRectangleCssClipPath(size, density)
    is Outline.Rounded -> toRoundedCssClipPath(size, density)
    is Outline.Generic -> toGenericCssClipPath(density)
  }
}

private fun Outline.Rectangle.toRectangleCssClipPath(size: Size, density: Float): String {
  return "inset(${videoClipInset(size, density)})"
}

private fun videoClipInset(size: Size, density: Float): String {
  val overlap = VideoClipOverlapDevicePixels / density
  return "-${overlap}px calc(100% - ${size.width / density + overlap}px) " +
      "calc(100% - ${size.height / density + overlap}px) -${overlap}px"
}

private fun Outline.Rounded.toRoundedCssClipPath(size: Size, density: Float): String {
  val overlap = VideoClipOverlapDevicePixels / density
  val corners =
      listOf(
          roundRect.topLeftCornerRadius,
          roundRect.topRightCornerRadius,
          roundRect.bottomRightCornerRadius,
          roundRect.bottomLeftCornerRadius,
      )
  val horizontal = corners.joinToString(" ") { "${it.x / density + overlap}px" }
  val vertical = corners.joinToString(" ") { "${it.y / density + overlap}px" }
  return "inset(${videoClipInset(size, density)} round $horizontal / $vertical)"
}

private fun Outline.Generic.toGenericCssClipPath(density: Float): String {
  val path =
      Path().apply {
        addOutline(this@toGenericCssClipPath)
        transform(Matrix().apply { scale(1 / density, 1 / density) })
      }
  return "path('${path.toSvg()}')"
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
