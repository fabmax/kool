package de.fabmax.kool.input

import de.fabmax.kool.JsImpl
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.configJs
import de.fabmax.kool.math.*
import de.fabmax.kool.platform.JsContext
import de.fabmax.kool.platform.TouchEvent
import de.fabmax.kool.platform.elementX
import de.fabmax.kool.platform.elementY
import de.fabmax.kool.util.logI
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.*

internal actual fun PlatformInput(): PlatformInput = PlatformInputJs

internal object PlatformInputJs : PlatformInput {

    val excludedKeyCodes = mutableSetOf("F5", "F11", "F12")

    private val virtualPointerPos = MutableVec2d()
    private var currentCursorShape = CursorShape.DEFAULT
    private var mouseButtonState = 0

    override fun setCursorMode(cursorMode: CursorMode) {
        PointerLockState.cursorMode = cursorMode
    }

    override fun applyCursorShape(cursorShape: CursorShape) {
        if (cursorShape != currentCursorShape) {
            JsImpl.canvas.style.cursor = when (cursorShape) {
                CursorShape.DEFAULT -> "default"
                CursorShape.TEXT -> "text"
                CursorShape.CROSSHAIR -> "crosshair"
                CursorShape.HAND -> "pointer"
                CursorShape.H_RESIZE -> "e-resize"
                CursorShape.V_RESIZE -> "n-resize"
            }
            currentCursorShape = cursorShape
        }
    }

    fun onContextCreated(ctx: JsContext) {
        installInputHandlers(ctx.canvas)
    }

    private fun Event.asTouchEvent(): TouchEvent? {
        return if (this is TouchEvent) this
        else null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun pointerMovementX(ev: MouseEvent) = js("ev.movementX") as Double * window.devicePixelRatio

    @Suppress("UNUSED_PARAMETER")
    private fun pointerMovementY(ev: MouseEvent) = js("ev.movementY") as Double * window.devicePixelRatio

    private fun installInputHandlers(canvas: HTMLCanvasElement) {

        var isTouchActive = false

        fun Event.buildTouchPosition(): Vec2d? {
            preventDefault()
            return with(canvas.getBoundingClientRect()) {
                asTouchEvent()?.let { touch ->
                    if (touch.changedTouches.length == 0) null
                    else {
                        val item = touch.changedTouches.item(0)
                        Vec2d(
                            item.elementX * window.devicePixelRatio - left,
                            item.elementY * window.devicePixelRatio - top
                        )
                    }
                }
            }
        }

        fun Event.invokeMoveTouchAsMouse() {
            buildTouchPosition()?.also {
                virtualPointerPos.x = it.x
                virtualPointerPos.y = it.y
                PointerInput.handleMouseMove(virtualPointerPos.x, virtualPointerPos.y)
            }
        }

        // install mouse handlers
        canvas.onmousemove = { ev ->
            if (!isTouchActive) {
                val bounds = canvas.getBoundingClientRect()
                if (PointerLockState.hasPointerLock) {
                    // on active pointer lock, mouse event position is constant and only deltas are reported
                    //  -> use deltas to compute a virtual unbounded pointer position
                    virtualPointerPos.x += pointerMovementX(ev)
                    virtualPointerPos.y += pointerMovementY(ev)
                } else {
                    virtualPointerPos.x = (ev.clientX * window.devicePixelRatio - bounds.left)
                    virtualPointerPos.y = (ev.clientY * window.devicePixelRatio - bounds.top)
                }
                PointerInput.handleMouseMove(virtualPointerPos.x, virtualPointerPos.y)
            }
        }

        canvas.onmousedown = { ev ->
            PointerLockState.checkLockState()
            val changeMask = ev.buttons.toInt() and mouseButtonState.inv()
            mouseButtonState = ev.buttons.toInt()
            for (btn in 0..7) {
                if (changeMask and (1 shl btn) != 0) {
                    PointerInput.handleMouseButtonEvent(btn, true)
                }
            }
        }
        canvas.onmouseup = { ev ->
            val changeMask = ev.buttons.toInt().inv() and mouseButtonState
            mouseButtonState = ev.buttons.toInt()
            for (btn in 0..7) {
                if (changeMask and (1 shl btn) != 0) {
                    PointerInput.handleMouseButtonEvent(btn, false)
                }
            }
        }
        canvas.onmouseleave = { PointerInput.handleMouseExit() }
        canvas.onwheel = { ev ->
            // scroll amount is browser dependent, try to norm it to roughly 1.0 ticks per mouse scroll wheel tick
            var yTicks = -ev.deltaY.toFloat() / 3.0
            var xTicks = -ev.deltaX.toFloat() / 3.0
            if (ev.deltaMode == 0) {
                // scroll delta is specified in pixels...
                yTicks /= 30.0
                xTicks /= 30.0
            }
            PointerInput.handleMouseScroll(xTicks, yTicks)
            ev.preventDefault()
        }

        document.addEventListener("pointerlockchange", { PointerLockState.onPointerLockChange(canvas) }, false)

        // install touch handlers
        canvas.addEventListener("touchstart", { ev ->
            ev.preventDefault()
            isTouchActive = true
            ev.invokeMoveTouchAsMouse()
            PointerInput.handleMouseButtonEvent(0, true)
        }, false)

        canvas.addEventListener("touchend", { ev ->
            ev.preventDefault()
            ev.invokeMoveTouchAsMouse()
            PointerInput.handleMouseButtonEvent(0, false)
            isTouchActive = false
        }, false)

        canvas.addEventListener("touchcancel", { ev ->
            ev.preventDefault()
            isTouchActive = false
            ev.invokeMoveTouchAsMouse()
            PointerInput.handleMouseButtonEvent(0, false)
            PointerInput.handleMouseExit()
            isTouchActive = false
        }, false)

        canvas.addEventListener("touchmove", { ev ->
            ev.preventDefault()
            ev.asTouchEvent()?.apply {
                if (changedTouches.length == 1) {
                    ev.invokeMoveTouchAsMouse()
                    if(!isTouchActive){
                        PointerInput.handleMouseButtonEvent(0, true)
                        isTouchActive = true
                    }
                }
            }
        }, false)

        if (KoolSystem.configJs.isGlobalKeyEventGrabbing) {
            document.onkeydown = { ev -> handleKeyDown(ev) }
            document.onkeyup = { ev -> handleKeyUp(ev) }
        } else {
            canvas.onkeydown = { ev -> handleKeyDown(ev) }
            canvas.onkeyup = { ev -> handleKeyUp(ev) }
        }
    }

    private fun handleKeyDown(ev: KeyboardEvent) {
        val keyCode = ev.toKeyCode()
        val localKeyCode = ev.toLocalKeyCode()
        var mods = 0
        if (keyCode.code != 0 || localKeyCode.code != 0) {
            if (ev.altKey) mods = mods or KeyboardInput.KEY_MOD_ALT
            if (ev.ctrlKey) mods = mods or KeyboardInput.KEY_MOD_CTRL
            if (ev.shiftKey) mods = mods or KeyboardInput.KEY_MOD_SHIFT
            if (ev.metaKey) mods = mods or KeyboardInput.KEY_MOD_SUPER

            var event = KeyboardInput.KEY_EV_DOWN
            if (ev.repeat) {
                event = event or KeyboardInput.KEY_EV_REPEATED
            }
            KeyboardInput.handleKeyEvent(KeyEvent(keyCode, localKeyCode, event, mods))
        }
        // do not issue an charType() if a modifier key is down (e.g. Ctrl+C), Shift is fine however (it's just
        // a capital letter then...)
        if (ev.key.length == 1 && (mods and KeyboardInput.KEY_MOD_SHIFT.inv()) == 0) {
            KeyboardInput.handleCharTyped(ev.key[0])
        }

        if (!excludedKeyCodes.contains(ev.code)) {
            ev.preventDefault()
        }
    }

    private fun handleKeyUp(ev: KeyboardEvent) {
        val keyCode = ev.toKeyCode()
        val localKeyCode = ev.toLocalKeyCode()
        if (keyCode.code != 0 || localKeyCode.code != 0) {
            var mods = 0
            if (ev.altKey) mods = mods or KeyboardInput.KEY_MOD_ALT
            if (ev.ctrlKey) mods = mods or KeyboardInput.KEY_MOD_CTRL
            if (ev.shiftKey) mods = mods or KeyboardInput.KEY_MOD_SHIFT
            if (ev.metaKey) mods = mods or KeyboardInput.KEY_MOD_SUPER
            KeyboardInput.handleKeyEvent(KeyEvent(keyCode, localKeyCode, KeyboardInput.KEY_EV_UP, mods))
        }

        if (!excludedKeyCodes.contains(ev.code)) {
            ev.preventDefault()
        }
    }

    private fun KeyboardEvent.toLocalKeyCode(): LocalKeyCode {
        val specialKey = KEY_CODE_MAP[code]
        return if (specialKey != null) {
            LocalKeyCode(specialKey.code, specialKey.name)
        } else {
            when (key.length) {
                1 -> LocalKeyCode(key[0].uppercaseChar().code)
                else -> LocalKeyCode(0)
            }
        }
    }

    private fun KeyboardEvent.toKeyCode(): UniversalKeyCode {
        return KEY_CODE_MAP[code] ?: when (key.length) {
            1 -> UniversalKeyCode(key[0].uppercaseChar().code)
            else -> UniversalKeyCode(0)
        }
    }

    val KEY_CODE_MAP: Map<String, UniversalKeyCode> = mutableMapOf(
        "ControlLeft" to KeyboardInput.KEY_CTRL_LEFT,
        "ControlRight" to KeyboardInput.KEY_CTRL_RIGHT,
        "ShiftLeft" to KeyboardInput.KEY_SHIFT_LEFT,
        "ShiftRight" to KeyboardInput.KEY_SHIFT_RIGHT,
        "AltLeft" to KeyboardInput.KEY_ALT_LEFT,
        "AltRight" to KeyboardInput.KEY_ALT_RIGHT,
        "MetaLeft" to KeyboardInput.KEY_SUPER_LEFT,
        "MetaRight" to KeyboardInput.KEY_SUPER_RIGHT,
        "Escape" to KeyboardInput.KEY_ESC,
        "ContextMenu" to KeyboardInput.KEY_MENU,
        "Enter" to KeyboardInput.KEY_ENTER,
        "NumpadEnter" to KeyboardInput.KEY_NP_ENTER,
        "NumpadDivide" to KeyboardInput.KEY_NP_DIV,
        "NumpadMultiply" to KeyboardInput.KEY_NP_MUL,
        "NumpadAdd" to KeyboardInput.KEY_NP_PLUS,
        "NumpadSubtract" to KeyboardInput.KEY_NP_MINUS,
        "NumpadDecimal" to KeyboardInput.KEY_NP_DECIMAL,
        "Backspace" to KeyboardInput.KEY_BACKSPACE,
        "Tab" to KeyboardInput.KEY_TAB,
        "Delete" to KeyboardInput.KEY_DEL,
        "Insert" to KeyboardInput.KEY_INSERT,
        "Home" to KeyboardInput.KEY_HOME,
        "End" to KeyboardInput.KEY_END,
        "PageUp" to KeyboardInput.KEY_PAGE_UP,
        "PageDown" to KeyboardInput.KEY_PAGE_DOWN,
        "ArrowLeft" to KeyboardInput.KEY_CURSOR_LEFT,
        "ArrowRight" to KeyboardInput.KEY_CURSOR_RIGHT,
        "ArrowUp" to KeyboardInput.KEY_CURSOR_UP,
        "ArrowDown" to KeyboardInput.KEY_CURSOR_DOWN,
        "F1" to KeyboardInput.KEY_F1,
        "F2" to KeyboardInput.KEY_F2,
        "F3" to KeyboardInput.KEY_F3,
        "F4" to KeyboardInput.KEY_F4,
        "F5" to KeyboardInput.KEY_F5,
        "F6" to KeyboardInput.KEY_F6,
        "F7" to KeyboardInput.KEY_F7,
        "F8" to KeyboardInput.KEY_F8,
        "F9" to KeyboardInput.KEY_F9,
        "F10" to KeyboardInput.KEY_F10,
        "F11" to KeyboardInput.KEY_F11,
        "F12" to KeyboardInput.KEY_F12,
        "Space" to UniversalKeyCode(' ')


        /*

        printableKeys += GLFW.GLFW_KEY_APOSTROPHE
        printableKeys += GLFW.GLFW_KEY_COMMA
        printableKeys += GLFW.GLFW_KEY_MINUS
        printableKeys += GLFW.GLFW_KEY_PERIOD
        printableKeys += GLFW.GLFW_KEY_SLASH
        printableKeys += GLFW.GLFW_KEY_SEMICOLON
        printableKeys += GLFW.GLFW_KEY_EQUAL
        printableKeys += GLFW.GLFW_KEY_LEFT_BRACKET
        printableKeys += GLFW.GLFW_KEY_RIGHT_BRACKET
        printableKeys += GLFW.GLFW_KEY_BACKSLASH
        //printableKeys += GLFW.GLFW_KEY_WORLD_1
        printableKeys += GLFW.GLFW_KEY_WORLD_2
        printableKeys += GLFW.GLFW_KEY_KP_DECIMAL
        printableKeys += GLFW.GLFW_KEY_KP_DIVIDE
        printableKeys += GLFW.GLFW_KEY_KP_MULTIPLY
        printableKeys += GLFW.GLFW_KEY_KP_SUBTRACT
        printableKeys += GLFW.GLFW_KEY_KP_ADD
        printableKeys += GLFW.GLFW_KEY_KP_EQUAL
         */

    )

    private object PointerLockState {
        var hasPointerLock = false
        var isApiExitRequest = false

        var cursorMode = CursorMode.NORMAL
            set(value) {
                field = value
                when (value) {
                    CursorMode.NORMAL -> exitPointerLock()
                    CursorMode.LOCKED -> requestPointerLock(JsImpl.canvas)
                }
            }

        @Suppress("UNUSED_PARAMETER")
        fun requestPointerLock(canvas: HTMLCanvasElement) {
            if (!hasPointerLock) {
                js("canvas.requestPointerLock()")
            }
        }

        fun exitPointerLock() {
            if (hasPointerLock) {
                isApiExitRequest = true
                js("document.exitPointerLock()")
            }
        }

        @Suppress("UNUSED_PARAMETER")
        fun onPointerLockChange(canvas: HTMLCanvasElement) {
            hasPointerLock = js("document.pointerLockElement === canvas") as Boolean

            if (!hasPointerLock && !isApiExitRequest) {
                // we lost pointer lock without requesting it via api -> user requested it by hitting the esc key
                // report an esc key-event, so the application can react on it
                logI { "pointer lock exited by user" }
                KeyboardInput.handleKeyEvent(KeyEvent(KeyboardInput.KEY_ESC, KeyboardInput.KEY_ESC, KeyboardInput.KEY_EV_DOWN, 0))
                KeyboardInput.handleKeyEvent(KeyEvent(KeyboardInput.KEY_ESC, KeyboardInput.KEY_ESC, KeyboardInput.KEY_EV_UP, 0))
            }
            isApiExitRequest = false
        }

        fun checkLockState() {
            if (cursorMode == CursorMode.LOCKED && !hasPointerLock) {
                // previous attempt to requestPointerLock() has failed, re-request it on user interaction
                requestPointerLock(JsImpl.canvas)
            }
        }
    }
}
