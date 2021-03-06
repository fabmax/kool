package de.fabmax.kool.platform

import de.fabmax.kool.InputManager
import de.fabmax.kool.JsImpl
import de.fabmax.kool.KoolContext
import de.fabmax.kool.KoolException
import de.fabmax.kool.pipeline.OffscreenRenderPass
import de.fabmax.kool.pipeline.OffscreenRenderPass2d
import de.fabmax.kool.pipeline.OffscreenRenderPassCube
import de.fabmax.kool.pipeline.shadermodel.ShaderGenerator
import de.fabmax.kool.platform.webgl.QueueRendererWebGl
import de.fabmax.kool.platform.webgl.ShaderGeneratorImplWebGl
import de.fabmax.kool.util.Viewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Float32Array
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLRenderingContext.Companion.MAX_TEXTURE_IMAGE_UNITS
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.UIEvent

/**
 * @author fabmax
 */
@Suppress("UnsafeCastFromDynamic")
class JsContext internal constructor(val props: InitProps) : KoolContext() {
    override val assetMgr = JsAssetManager(props.assetsBaseDir, this)

    override val shaderGenerator: ShaderGenerator = ShaderGeneratorImplWebGl()
    internal val queueRenderer = QueueRendererWebGl(this)
    internal val afterRenderActions = mutableListOf<() -> Unit>()

    override var windowWidth = 0
        private set
    override var windowHeight = 0
        private set

    private val canvas: HTMLCanvasElement
    internal val gl: WebGL2RenderingContext
    private val sysInfo = mutableListOf<String>()

    private var animationMillis = 0.0

    val glCapabilities = GlCapabilities()

    init {
        canvas = document.getElementById(props.canvasName) as HTMLCanvasElement
        // try to get a WebGL2 context first and use WebGL version 1 as fallback
        var webGlCtx = canvas.getContext("webgl2")
        if (webGlCtx == null) {
            webGlCtx = canvas.getContext("experimental-webgl2")
        }

        if (webGlCtx != null) {
            gl = webGlCtx as WebGL2RenderingContext
            sysInfo += "WebGL 2.0"

            glCapabilities.maxTexUnits = gl.getParameter(MAX_TEXTURE_IMAGE_UNITS).asDynamic()
            glCapabilities.hasFloatTextures = gl.getExtension("EXT_color_buffer_float") != null

        } else {
            js("alert(\"Unable to initialize WebGL2 context. Your browser may not support it.\")")
            throw KoolException("WebGL2 context required")
        }

        val extAnisotropic = gl.getExtension("EXT_texture_filter_anisotropic") ?:
                gl.getExtension("MOZ_EXT_texture_filter_anisotropic") ?:
                gl.getExtension("WEBKIT_EXT_texture_filter_anisotropic")
        if (extAnisotropic != null) {
            glCapabilities.maxAnisotropy = gl.getParameter(extAnisotropic.MAX_TEXTURE_MAX_ANISOTROPY_EXT) as Int
            glCapabilities.glTextureMaxAnisotropyExt = extAnisotropic.TEXTURE_MAX_ANISOTROPY_EXT
        }

        screenDpi = JsImpl.dpi
        windowWidth = canvas.clientWidth
        windowHeight = canvas.clientHeight

        // suppress context menu
        canvas.oncontextmenu = Event::preventDefault

        // install mouse handlers
        canvas.onmousemove = { ev ->
            val bounds = canvas.getBoundingClientRect()
            val x = (ev.clientX - bounds.left).toFloat()
            val y = (ev.clientY - bounds.top).toFloat()
            inputMgr.handleMouseMove(x, y)
        }
        canvas.onmousedown = { ev ->
            inputMgr.handleMouseButtonStates(ev.buttons.toInt())
        }
        canvas.onmouseup = { ev ->
            inputMgr.handleMouseButtonStates(ev.buttons.toInt())
        }
        canvas.onmouseleave = { inputMgr.handleMouseExit() }
        canvas.onwheel = { ev ->
            // scroll amount is browser dependent, try to norm it to roughly 1.0 ticks per mouse
            // scroll wheel tick
            var ticks = -ev.deltaY.toFloat() / 3f
            if (ev.deltaMode == 0) {
                // scroll delta is specified in pixels...
                ticks /= 30f
            }
            inputMgr.handleMouseScroll(ticks)
            ev.preventDefault()
        }

        // install touch handlers
        canvas.addEventListener("touchstart", { ev ->
            ev.preventDefault()
            val changedTouches = (ev as TouchEvent).changedTouches
            for (i in 0 until changedTouches.length) {
                val touch = changedTouches.item(i)
                inputMgr.handleTouchStart(touch.identifier, touch.elementX, touch.elementY)
            }
        }, false)
        canvas.addEventListener("touchend", { ev ->
            ev.preventDefault()
            val changedTouches = (ev as TouchEvent).changedTouches
            for (i in 0 until changedTouches.length) {
                val touch = changedTouches.item(i)
                inputMgr.handleTouchEnd(touch.identifier)
            }
        }, false)
        canvas.addEventListener("touchcancel", { ev ->
            ev.preventDefault()
            val changedTouches = (ev as TouchEvent).changedTouches
            for (i in 0 until changedTouches.length) {
                val touch = changedTouches.item(i)
                inputMgr.handleTouchCancel(touch.identifier)
            }
        }, false)
        canvas.addEventListener("touchmove", { ev ->
            ev.preventDefault()
            val changedTouches = (ev as TouchEvent).changedTouches
            for (i in 0 until changedTouches.length) {
                val touch = changedTouches.item(i)
                inputMgr.handleTouchMove(touch.identifier, touch.elementX, touch.elementY)
            }
        }, false)

        document.onkeydown = { ev -> handleKeyDown(ev) }
        document.onkeyup = { ev -> handleKeyUp(ev) }

//        if (canvas.tabIndex <= 0) {
//            println("No canvas tabIndex set! Falling back to document key events, this doesn't work with multi context")
//        } else {
//            canvas.onkeydown = { ev -> handleKeyDown(ev as KeyboardEvent) }
//            canvas.onkeyup = { ev -> handleKeyUp(ev as KeyboardEvent) }
//        }
    }

    private fun handleKeyDown(ev: KeyboardEvent) {
        val code = translateKeyCode(ev.code)
        if (code != 0) {
            var mods = 0
            if (ev.altKey) { mods = mods or InputManager.KEY_MOD_ALT }
            if (ev.ctrlKey) { mods = mods or InputManager.KEY_MOD_CTRL }
            if (ev.shiftKey) { mods = mods or InputManager.KEY_MOD_SHIFT }
            if (ev.metaKey) { mods = mods or InputManager.KEY_MOD_SUPER }

            var event = InputManager.KEY_EV_DOWN
            if (ev.repeat) {
                event = event or InputManager.KEY_EV_REPEATED
            }
            inputMgr.keyEvent(code, mods, event)
        }
        if (ev.key.length == 1) {
            inputMgr.charTyped(ev.key[0])
        }

        if (!props.excludedKeyCodes.contains(ev.code)) {
            ev.preventDefault()
        }
    }

    private fun handleKeyUp(ev: KeyboardEvent) {
        val code = translateKeyCode(ev.code)
        if (code != 0) {
            var mods = 0
            if (ev.altKey) { mods = mods or InputManager.KEY_MOD_ALT }
            if (ev.ctrlKey) { mods = mods or InputManager.KEY_MOD_CTRL }
            if (ev.shiftKey) { mods = mods or InputManager.KEY_MOD_SHIFT }
            if (ev.metaKey) { mods = mods or InputManager.KEY_MOD_SUPER }

            inputMgr.keyEvent(code, mods, InputManager.KEY_EV_UP)
        }

        if (!props.excludedKeyCodes.contains(ev.code)) {
            ev.preventDefault()
        }
    }

    private fun translateKeyCode(code: String): Int {
        if (code.length == 4 && code.startsWith("Key")) {
            return code[3].toInt()
        } else {
            return KEY_CODE_MAP[code] ?: 0
        }
    }

    private fun renderFrame(time: Double) {
        // determine delta time
        val dt = (time - animationMillis) / 1000.0
        animationMillis = time

        // update viewport size
        windowWidth = canvas.clientWidth
        windowHeight = canvas.clientHeight
        if (windowWidth != canvas.width || windowHeight!= canvas.height) {
            // resize canvas to viewport
            canvas.width = windowWidth
            canvas.height = windowHeight
        }

        // render frame
        render(dt)
        draw()
        gl.finish()

        // request next frame
        window.requestAnimationFrame { t -> renderFrame(t) }
    }

    private fun draw() {
        if (disposablePipelines.isNotEmpty()) {
            queueRenderer.disposePipelines(disposablePipelines)
            disposablePipelines.clear()
        }

        engineStats.resetPerFrameCounts()

        for (j in backgroundPasses.indices) {
            if (backgroundPasses[j].isEnabled) {
                drawOffscreen(backgroundPasses[j])
                backgroundPasses[j].afterDraw(this)
            }
        }
        for (i in scenes.indices) {
            val scene = scenes[i]
            if (scene.isVisible) {
                for (j in scene.offscreenPasses.indices) {
                    if (scene.offscreenPasses[j].isEnabled) {
                        drawOffscreen(scene.offscreenPasses[j])
                        scene.offscreenPasses[j].afterDraw(this)
                    }
                }
                queueRenderer.renderQueue(scene.mainRenderPass.drawQueue)
                scene.mainRenderPass.afterDraw(this)
            }
        }

        if (afterRenderActions.isNotEmpty()) {
            afterRenderActions.forEach { it() }
            afterRenderActions.clear()
        }
    }

    private fun drawOffscreen(offscreenPass: OffscreenRenderPass) {
        when (offscreenPass) {
            is OffscreenRenderPass2d -> offscreenPass.impl.draw(this)
            is OffscreenRenderPassCube -> offscreenPass.impl.draw(this)
            else -> throw IllegalArgumentException("Offscreen pass type not implemented: $offscreenPass")
        }
    }

    override fun openUrl(url: String) {
        window.open(url)
    }

    override fun run() {
        window.requestAnimationFrame { t -> renderFrame(t) }
    }

    override fun destroy() {
        // nothing to do here...
    }

    override fun getSysInfos(): List<String> {
        return sysInfo
    }

    override fun getWindowViewport(result: Viewport) {
        result.set(0, 0, windowWidth, windowHeight)
    }

    class InitProps {
        var canvasName = "glCanvas"
        val excludedKeyCodes: MutableSet<String> = mutableSetOf("F5")

        var assetsBaseDir = "./assets"
    }

    companion object {
        val KEY_CODE_MAP: Map<String, Int> = mutableMapOf(
                "ControlLeft" to InputManager.KEY_CTRL_LEFT,
                "ControlRight" to InputManager.KEY_CTRL_RIGHT,
                "ShiftLeft" to InputManager.KEY_SHIFT_LEFT,
                "ShiftRight" to InputManager.KEY_SHIFT_RIGHT,
                "AltLeft" to InputManager.KEY_ALT_LEFT,
                "AltRight" to InputManager.KEY_ALT_RIGHT,
                "MetaLeft" to InputManager.KEY_SUPER_LEFT,
                "MetaRight" to InputManager.KEY_SUPER_RIGHT,
                "Escape" to InputManager.KEY_ESC,
                "ContextMenu" to InputManager.KEY_MENU,
                "Enter" to InputManager.KEY_ENTER,
                "NumpadEnter" to InputManager.KEY_NP_ENTER,
                "NumpadDivide" to InputManager.KEY_NP_DIV,
                "NumpadMultiply" to InputManager.KEY_NP_MUL,
                "NumpadAdd" to InputManager.KEY_NP_PLUS,
                "NumpadSubtract" to InputManager.KEY_NP_MINUS,
                "Backspace" to InputManager.KEY_BACKSPACE,
                "Tab" to InputManager.KEY_TAB,
                "Delete" to InputManager.KEY_DEL,
                "Insert" to InputManager.KEY_INSERT,
                "Home" to InputManager.KEY_HOME,
                "End" to InputManager.KEY_END,
                "PageUp" to InputManager.KEY_PAGE_UP,
                "PageDown" to InputManager.KEY_PAGE_DOWN,
                "ArrowLeft" to InputManager.KEY_CURSOR_LEFT,
                "ArrowRight" to InputManager.KEY_CURSOR_RIGHT,
                "ArrowUp" to InputManager.KEY_CURSOR_UP,
                "ArrowDown" to InputManager.KEY_CURSOR_DOWN,
                "F1" to InputManager.KEY_F1,
                "F2" to InputManager.KEY_F2,
                "F3" to InputManager.KEY_F3,
                "F4" to InputManager.KEY_F4,
                "F5" to InputManager.KEY_F5,
                "F6" to InputManager.KEY_F6,
                "F7" to InputManager.KEY_F7,
                "F8" to InputManager.KEY_F8,
                "F9" to InputManager.KEY_F9,
                "F10" to InputManager.KEY_F10,
                "F11" to InputManager.KEY_F11,
                "F12" to InputManager.KEY_F12,
                "Space" to ' '.toInt()
        )
    }
}

class GlCapabilities {
    var maxTexUnits = 16
        internal set
    var hasFloatTextures = false
        internal set
    var maxAnisotropy = 1
        internal set
    var glTextureMaxAnisotropyExt = 0
        internal set
}

external class TouchEvent: UIEvent {
    val altKey: Boolean
    val changedTouches: TouchList
    val ctrlKey: Boolean
    val metaKey: Boolean
    val shiftKey: Boolean
    val targetTouches: TouchList
    val touches: TouchList
}

external class TouchList {
    val length: Int
    fun item(index: Int): Touch
}

external class Touch {
    val identifier: Int
    val screenX: Float
    val screenY: Float
    val clientX: Float
    val clientY: Float
    val pageX: Float
    val pageY: Float
    val target: Element
    val radiusX: Float
    val radiusY: Float
    val rotationAngle: Float
    val force: Float
}

abstract external class WebGL2RenderingContext : WebGLRenderingContext {
    fun bufferData(target: Int, srcData: ArrayBufferView, usage: Int, srcOffset: Int, length: Int)
    fun clearBufferfv(buffer: Int, drawBuffer: Int, values: Float32Array)
    fun drawBuffers(buffers: IntArray)
    fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int)
    fun readBuffer(src: Int)
    fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int)
    fun texImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, type: Int, srcData: ArrayBufferView?)
    fun texImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, type: Int, source: HTMLImageElement?)
    fun texSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, type: Int, pixels: ImageData?)
    fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun texStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int)
    fun vertexAttribDivisor(index: Int, divisor: Int)
    fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)

    companion object {
        val COLOR: Int
        val DEPTH: Int
        val STENCIL: Int
        val DEPTH_STENCIL: Int

        val DEPTH_COMPONENT24: Int
        val TEXTURE_3D: Int
        val TEXTURE_WRAP_R: Int
        val TEXTURE_COMPARE_MODE: Int
        val COMPARE_REF_TO_TEXTURE: Int
        val TEXTURE_COMPARE_FUNC: Int

        val RED: Int
        val RG: Int

        val R8: Int
        val RG8: Int
        val RGB8: Int
        val RGBA8: Int

        val R8UI: Int
        val RG8UI: Int
        val RGB8UI: Int
        val RGBA8UI: Int

        val R16F: Int
        val RG16F: Int
        val RGB16F: Int
        val RGBA16F: Int
    }
}

val Touch.elementX: Float
    get() = clientX - ((target as? HTMLCanvasElement)?.clientLeft?.toFloat() ?: 0f)

val Touch.elementY: Float
    get() = clientY - ((target as? HTMLCanvasElement)?.clientTop?.toFloat() ?: 0f)
