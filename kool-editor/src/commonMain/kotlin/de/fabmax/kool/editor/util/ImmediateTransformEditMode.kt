package de.fabmax.kool.editor.util

import de.fabmax.kool.KoolContext
import de.fabmax.kool.editor.EditorEditMode
import de.fabmax.kool.editor.KoolEditor
import de.fabmax.kool.input.*
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.gizmo.*
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.TrsTransformD

class ImmediateTransformEditMode(val editor: KoolEditor) : InputStack.PointerListener {
    private val mode: MutableStateValue<EditorEditMode.Mode> get() = editor.editMode.mode

    private val gizmo = GizmoNode()
    private val globalRay = RayD()
    private val localRay = RayD()

    private val globalToDragLocal = MutableMat4d()
    private val clientGlobalToParent = MutableMat4d()
    private val clientTransformOffset = MutableMat4d()

    private var selectionTransform: SelectionTransform? = null
    private var dragCtxStart: DragContext? = null

    private var activeOp: GizmoOperation? = null

    private val opCamPlaneTranslate = CamPlaneTranslation()
    private val opXAxisTranslate = AxisTranslation(GizmoHandle.Axis.POS_X)
    private val opYAxisTranslate = AxisTranslation(GizmoHandle.Axis.POS_Y)
    private val opZAxisTranslate = AxisTranslation(GizmoHandle.Axis.POS_Z)
    private val opXPlaneTranslate = PlaneTranslation(Vec3d.X_AXIS)
    private val opYPlaneTranslate = PlaneTranslation(Vec3d.Y_AXIS)
    private val opZPlaneTranslate = PlaneTranslation(Vec3d.Z_AXIS)

    private val opCamPlaneRotate = CamPlaneRotation()
    private val opXAxisRotate = AxisRotation(GizmoHandle.Axis.POS_X)
    private val opYAxisRotate = AxisRotation(GizmoHandle.Axis.POS_Y)
    private val opZAxisRotate = AxisRotation(GizmoHandle.Axis.POS_Z)

    private val opUniformScale = UniformScale()
    private val opXAxisScale = AxisScale(GizmoHandle.Axis.POS_X)
    private val opYAxisScale = AxisScale(GizmoHandle.Axis.POS_Y)
    private val opZAxisScale = AxisScale(GizmoHandle.Axis.POS_Z)
    private val opXPlaneScale = PlaneScale(Vec3d.X_AXIS)
    private val opYPlaneScale = PlaneScale(Vec3d.Y_AXIS)
    private val opZPlaneScale = PlaneScale(Vec3d.Z_AXIS)

    val isActive: Boolean get() = gizmo.isManipulating

    private val inputHandler = object : InputStack.InputHandler("Immediate transform mode") {
        init {
            blockAllKeyboardInput = true
            pointerListeners += this@ImmediateTransformEditMode

            addKeyListener(name = "Cancel transform", keyCode = KeyboardInput.KEY_ESC) {
                finish(isCanceled = true)
                mode.set(EditorEditMode.Mode.NONE)
            }

            addKeyListener(name = "Move", keyCode = LocalKeyCode('g')) {
                mode.set(EditorEditMode.Mode.MOVE_IMMEDIATE)
                setOp(opCamPlaneTranslate)
            }
            addKeyListener(name = "Rotate", keyCode = LocalKeyCode('r')) {
                mode.set(EditorEditMode.Mode.ROTATE_IMMEDIATE)
                setOp(opCamPlaneRotate)
            }
            addKeyListener(name = "Scale", keyCode = LocalKeyCode('s')) {
                mode.set(EditorEditMode.Mode.SCALE_IMMEDIATE)
                setOp(opUniformScale)
            }

            addKeyListener(name = "X-axis", keyCode = LocalKeyCode('x'), filter = FILTER_NO_SHIFT) { setXAxisOp() }
            addKeyListener(name = "Y-axis", keyCode = LocalKeyCode('y'), filter = FILTER_NO_SHIFT) { setYAxisOp() }
            addKeyListener(name = "Z-axis", keyCode = LocalKeyCode('z'), filter = FILTER_NO_SHIFT) { setZAxisOp() }

            addKeyListener(name = "X-plane", keyCode = LocalKeyCode('x'), filter = FILTER_SHIFT) { setXPlaneOp() }
            addKeyListener(name = "Y-plane", keyCode = LocalKeyCode('y'), filter = FILTER_SHIFT) { setYPlaneOp() }
            addKeyListener(name = "Z-plane", keyCode = LocalKeyCode('z'), filter = FILTER_SHIFT) { setZPlaneOp() }
        }
    }

    private fun setXAxisOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opXAxisTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opXAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opXAxisScale)
            else -> { }
        }
    }

    private fun setYAxisOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opYAxisTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opYAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opYAxisScale)
            else -> { }
        }
    }

    private fun setZAxisOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opZAxisTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opZAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opZAxisScale)
            else -> { }
        }
    }

    private fun setXPlaneOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opXPlaneTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opXAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opXAxisScale)
            else -> { }
        }
    }

    private fun setYPlaneOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opYPlaneTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opYAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opYAxisScale)
            else -> { }
        }
    }

    private fun setZPlaneOp() {
        when (mode.value) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> setOp(opZPlaneTranslate)
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> setOp(opZAxisRotate)
            EditorEditMode.Mode.SCALE_IMMEDIATE -> setOp(opZAxisScale)
            else -> { }
        }
    }

    private fun setOp(op: GizmoOperation) {
        activeOp = op
        if (isActive) {
            dragCtxStart?.let { activeOp?.onDragStart(it) }
        }
    }

    fun start(mode: EditorEditMode.Mode) {
        if (isActive) {
            return
        }

        activeOp = when (mode) {
            EditorEditMode.Mode.MOVE_IMMEDIATE -> opCamPlaneTranslate
            EditorEditMode.Mode.ROTATE_IMMEDIATE -> opCamPlaneRotate
            EditorEditMode.Mode.SCALE_IMMEDIATE -> opUniformScale
            else -> opCamPlaneTranslate
        }

        selectionTransform = SelectionTransform(editor.selectionOverlay.getSelectedSceneNodes())
        val primNode = selectionTransform?.primaryTransformNode ?: return

        updateGizmoFromClient(primNode.drawNode)
        globalToDragLocal.set(primNode.drawNode.invModelMatD)
        selectionTransform?.startTransform()

        InputStack.pushTop(inputHandler)
    }

    fun finish(isCanceled: Boolean) {
        if (gizmo.isManipulating) {
            if (isCanceled) {
                gizmo.cancelManipulation()
                // restore original / start transform
                updateFromGizmo(gizmo.gizmoTransform)
                selectionTransform?.updateTransform()
                selectionTransform?.applyTransform(false)

            } else {
                gizmo.finishManipulation()
                selectionTransform?.applyTransform(true)
            }
        }
        selectionTransform = null

        InputStack.remove(inputHandler)
    }

    private fun updateGizmoFromClient(client: Node) {
        clientGlobalToParent.set(client.parent?.invModelMatD ?: Mat4d.IDENTITY)
        clientTransformOffset.setIdentity()

        val translation = client.modelMatD.transform(MutableVec3d(), 1.0)
        val rotation = MutableQuatD(QuatD.IDENTITY)

        when (editor.gizmoOverlay.transformFrame.value) {
            GizmoFrame.LOCAL -> {
                client.modelMatD.decompose(rotation = rotation)
                gizmo.gizmoTransform.setCompositionOf(translation, rotation)
                val localScale = MutableVec3d()
                client.modelMatD.decompose(scale = localScale)
                clientTransformOffset.scale(localScale)
            }
            GizmoFrame.PARENT -> {
                client.parent?.modelMatD?.decompose(rotation = rotation)
                gizmo.gizmoTransform.setCompositionOf(translation, rotation)
                val localRotation = MutableQuatD()
                val localScale = MutableVec3d()
                client.transform.decompose(rotation = localRotation)
                client.modelMatD.decompose(scale = localScale)
                clientTransformOffset.rotate(localRotation).scale(localScale)
            }
            GizmoFrame.GLOBAL -> {
                gizmo.gizmoTransform.setCompositionOf(translation)
                val localRotation = MutableQuatD()
                val localScale = MutableVec3d()
                client.modelMatD.decompose(rotation = localRotation, scale = localScale)
                clientTransformOffset.rotate(localRotation).scale(localScale)
            }
        }
    }

    private fun updateFromGizmo(transform: TrsTransformD) {
        val client = selectionTransform?.primaryTransformNode?.drawNode ?: return

        val localTransform = MutableMat4d().set(Mat4d.IDENTITY)
            .mul(clientGlobalToParent)
            .mul(transform.matrixD)
            .mul(clientTransformOffset)
        client.transform.setMatrix(localTransform)
    }

    override fun handlePointer(pointerState: PointerState, ctx: KoolContext) {
        val ptr = pointerState.primaryPointer
        val scene = editor.editorContent.findParentOfType<Scene>()
        if (scene == null || !scene.computePickRay(ptr, globalRay)) {
            return
        }

        globalRay.transformBy(globalToDragLocal, localRay)
        val dragCtx = DragContext(gizmo, ptr, globalRay, localRay, globalToDragLocal, scene.camera)

        if (!gizmo.isManipulating) {
            dragCtxStart = dragCtx
            activeOp?.onDragStart(dragCtx)
        } else {
            activeOp?.onDrag(dragCtx)
            updateFromGizmo(gizmo.gizmoTransform)
            selectionTransform?.updateTransform()
            selectionTransform?.applyTransform(false)
        }

        if (ptr.isLeftButtonClicked) {
            ptr.consume()
            finish(false)
            mode.set(EditorEditMode.Mode.NONE)

        } else if (ptr.isRightButtonClicked) {
            ptr.consume()
            finish(true)
            mode.set(EditorEditMode.Mode.NONE)
        }
    }

    companion object {
        private val FILTER_NO_SHIFT: (KeyEvent) -> Boolean = { it.isPressed && !it.isShiftDown }
        private val FILTER_SHIFT: (KeyEvent) -> Boolean = { it.isPressed && it.isShiftDown }
    }
}