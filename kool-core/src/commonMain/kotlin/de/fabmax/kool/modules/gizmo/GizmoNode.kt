package de.fabmax.kool.modules.gizmo

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.PointerState
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Camera
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.util.BufferedList

class GizmoNode(name: String = "gizmo") : Node(name), InputStack.PointerListener {

    val gizmoTransform = TrsTransformD()

    val gizmoListeners = BufferedList<GizmoListener>()

    private val nodeTransform = TrsTransformD()
    private val handleTransform = TrsTransformD()

    private val startTransform = TrsTransformD()
    private val startScale = MutableVec3d()

    private val handleGroup = Node().apply {
        transform = handleTransform
    }

    private val rayTest = RayTest()
    private val pickRay = RayD()
    private var dragMode = DragMode.NO_DRAG
    private var isDrag = false
    private var hoverHandle: GizmoHandle? = null

    private val globalToDragLocal = MutableMat4d()
    private val gizmoRotation = MutableMat3d()

    private val escListener = InputStack.SimpleKeyListener(KeyboardInput.KEY_ESC, "Cancel drag") {
        cancelManipulation()
    }

    var isDistanceIndependentSize = true
    var gizmoSize = 1f
        set(value) {
            field = value
            handleTransform.scale(value)
        }

    private var parentCam: Camera? = null
    private val camUpdateListener: (RenderPass.UpdateEvent) -> Unit = { ev ->
        gizmoTransform.decompose(nodeTransform.translation, nodeTransform.rotation)
        nodeTransform.markDirty()

        if (isDistanceIndependentSize) {
            val cam = ev.camera
            val handleOrigin = handleGroup.modelMatF.transform(MutableVec3f(), 1f)
            val distance = (handleOrigin - cam.globalPos) dot cam.globalLookDir
            handleTransform.setIdentity().scale(distance / 10f * gizmoSize)
        } else {
            handleTransform.scale(gizmoSize)
        }
        updateModelMatRecursive()
        if (isManipulating) {
            gizmoListeners.forEach { it.onGizmoUpdate(gizmoTransform) }
        }
    }

    var isManipulating = false
        private set

    init {
        transform = nodeTransform
        drawGroupId = DEFAULT_GIZMO_DRAW_GROUP
        addNode(handleGroup)

        onUpdate { ev ->
            gizmoListeners.update()
            if (parentCam != ev.camera) {
                parentCam?.let { it.onCameraUpdated -= camUpdateListener }
                parentCam = ev.camera
                ev.camera.onCameraUpdated += camUpdateListener
            }
        }
    }

    override fun release() {
        super.release()
        parentCam?.let { it.onCameraUpdated -= camUpdateListener }
    }

    fun addHandle(handle: GizmoHandle) {
        handleGroup.addNode(handle.drawNode)
    }

    fun removeHandle(handle: GizmoHandle) {
        handleGroup.removeNode(handle.drawNode)
    }

    fun clearHandles() {
        handleGroup.children.forEach { it.release() }
        handleGroup.clearChildren()
    }

    fun startManipulation(cancelOnEscape: Boolean = true) {
        startTransform.set(gizmoTransform)
        isManipulating = true
        gizmoListeners.forEach { it.onManipulationStart(startTransform) }

        if (cancelOnEscape) {
            InputStack.defaultInputHandler.addKeyListener(escListener)
        }
    }

    fun finishManipulation() {
        check(isManipulating) { "finishManipulation is only allowed after calling startManipulation()" }

        isManipulating = false
        gizmoListeners.forEach { it.onManipulationFinished(startTransform, gizmoTransform) }
        InputStack.defaultInputHandler.removeKeyListener(escListener)
    }

    fun cancelManipulation() {
        check(isManipulating) { "cancelManipulation is only allowed after calling startManipulation()" }

        gizmoTransform.set(startTransform)
        isManipulating = false
        gizmoListeners.forEach { it.onManipulationCanceled(startTransform) }
        InputStack.defaultInputHandler.removeKeyListener(escListener)
    }

    fun manipulateAxisTranslation(axis: GizmoHandle.Axis, distance: Double) {
        check(isManipulating) { "manipulateAxisTranslation is only allowed between calling startManipulation() and finishManipulation()" }

        // gizmoTransform is TRS, i.e. translation is applied before rotation. Rotate given translation to current
        // gizmo orientation
        gizmoRotation.setIdentity().rotate(gizmoTransform.rotation)
        val rotatedAxis = gizmoRotation.transform(axis.axis, MutableVec3d())

        gizmoTransform.set(startTransform)
        gizmoTransform.translate(rotatedAxis * distance)
    }

    fun manipulateTranslation(translationOffset: Vec3d) {
        check(isManipulating) { "manipulateAxisTranslation is only allowed between calling startManipulation() and finishManipulation()" }

        // gizmoTransform is TRS, i.e. translation is applied before rotation. Rotate given translation to current
        // gizmo orientation
        gizmoRotation.setIdentity().rotate(gizmoTransform.rotation)
        val rotatedTranslation = gizmoRotation.transform(translationOffset, MutableVec3d())

        gizmoTransform.set(startTransform)
        gizmoTransform.translate(rotatedTranslation)
    }

    fun manipulateAxisRotation(axis: Vec3d, angle: AngleD) {
        check(isManipulating) { "manipulateAxisRotation is only allowed between calling startManipulation() and finishManipulation()" }

        gizmoTransform.set(startTransform)
        gizmoTransform.rotate(angle, axis)
    }

    fun manipulateRotation(rotation: QuatD) {
        check(isManipulating) { "manipulateAxisRotation is only allowed between calling startManipulation() and finishManipulation()" }

        gizmoTransform.set(startTransform)
        gizmoTransform.rotate(rotation)
    }

    fun manipulateScale(scale: Vec3d) {
        check(isManipulating) { "manipulateAxisTranslation is only allowed between calling startManipulation() and finishManipulation()" }

        gizmoTransform.set(startTransform)
        gizmoTransform.scale(scale)
    }

    override fun handlePointer(pointerState: PointerState, ctx: KoolContext) {
        if (!isVisibleRecursive()) {
            return
        }

        val ptr = pointerState.primaryPointer
        val scene = findParentOfType<Scene>()
        if (scene == null || !scene.computePickRay(ptr, pickRay)) {
            return
        }

        rayTest.clear()
        pickRay.toRayF(rayTest.ray)
        rayTest(rayTest)

        if (dragMode == DragMode.NO_DRAG) {
            val newHandle = if (rayTest.isHit) {
                rayTest.hitNode?.findParentOfType<GizmoHandle>()
            } else {
                null
            }
            if (newHandle != hoverHandle) {
                hoverHandle?.onHoverExit(this)
            }
            hoverHandle = newHandle
        }

        if (dragMode == DragMode.NO_DRAG && ptr.isLeftButtonDown) {
            dragMode = if (hoverHandle != null) DragMode.DRAG_MANIPULATE else DragMode.DRAG_IGNORE
        } else if (!ptr.isLeftButtonDown) {
            dragMode = DragMode.NO_DRAG
        }

        hoverHandle?.let { hover ->
            if (ptr.isLeftButtonDown && !isDrag) {
                globalToDragLocal.set(invModelMatD)
            }
            val dragCtx = DragContext(
                gizmo = this,
                pointer = ptr,
                globalRay = pickRay,
                localRay = pickRay.transformBy(globalToDragLocal, RayD()),
                globalToLocal = globalToDragLocal,
                camera = scene.camera
            )

            if (ptr.isLeftButtonDown) {
                ptr.consume()
                if (!isDrag) {
                    hover.onDragStart(dragCtx)
                    isDrag = true
                } else {
                    hover.onDrag(dragCtx)
                }
            } else {
                if (isDrag) {
                    hover.onDragEnd(dragCtx)
                    isDrag = false
                }
                hover.onHover(ptr, pickRay, this)
            }
        }

        if (!ptr.isLeftButtonDown) {
            isDrag = false
        }
    }

    private fun isVisibleRecursive(): Boolean {
        var it: Node? = this
        while (it != null) {
            if (!it.isVisible) {
                return false
            }
            it = it.parent
        }
        return true
    }

    companion object {
        const val DEFAULT_GIZMO_DRAW_GROUP = 1000
    }

    private enum class DragMode {
        NO_DRAG,
        DRAG_MANIPULATE,
        DRAG_IGNORE
    }
}
