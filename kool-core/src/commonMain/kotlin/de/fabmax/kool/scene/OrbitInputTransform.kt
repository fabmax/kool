package de.fabmax.kool.scene

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.PointerState
import de.fabmax.kool.math.*
import de.fabmax.kool.math.spatial.BoundingBoxD
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.animation.SpringDamperDouble
import de.fabmax.kool.util.Time

/**
 * A special kind of transform group which translates mouse input into a orbit transform. This is mainly useful
 * for camera manipulation.
 *
 * @author fabmax
 */

fun orbitCamera(view: RenderPass.View, name: String? = null, block: OrbitInputTransform.() -> Unit): OrbitInputTransform {
    val orbitCam = OrbitInputTransform(name)
    orbitCam.addNode(view.camera)
    orbitCam.block()
    view.drawNode.addNode(orbitCam)

    InputStack.defaultInputHandler.pointerListeners += orbitCam
    orbitCam.onRelease {
        InputStack.defaultInputHandler.pointerListeners -= orbitCam
    }
    return orbitCam
}

fun Scene.orbitCamera(name: String? = null, block: OrbitInputTransform.() -> Unit): OrbitInputTransform {
    return orbitCamera(mainRenderPass.screenView, name, block)
}

fun Scene.defaultOrbitCamera(yaw: Float = 20f, pitch: Float = -30f): OrbitInputTransform {
    return orbitCamera {
        setMouseRotation(yaw, pitch)
    }
}

open class OrbitInputTransform(name: String? = null) : Node(name), InputStack.PointerListener {
    var leftDragMethod = DragMethod.ROTATE
    var middleDragMethod = DragMethod.NONE
    var rightDragMethod = DragMethod.PAN
    var zoomMethod = ZoomMethod.ZOOM_CENTER
    var isConsumingPtr = true

    var verticalAxis = Vec3d.Y_AXIS
    var horizontalAxis = Vec3d.X_AXIS
    var minHorizontalRot = -90.0
    var maxHorizontalRot = 90.0

    val translation = MutableVec3d()
    var verticalRotation = 0.0
    var horizontalRotation = 0.0
    var zoom = 10.0
        set(value) {
            field = value.clamp(minZoom, maxZoom)
        }

    var isKeepingStandardTransform = false

    var invertRotX = false
    var invertRotY = false

    var minZoom = 1.0
    var maxZoom = 100.0
    var translationBounds: BoundingBoxD? = null

    var panMethod: PanBase = CameraOrthogonalPan()

    val vertRotAnimator = SpringDamperDouble(0.0)
    val horiRotAnimator = SpringDamperDouble(0.0)
    val zoomAnimator = SpringDamperDouble(zoom)

    private var prevButtonMask = 0
    private var dragMethod = DragMethod.NONE
    private var dragStart = false
    private val deltaPos = MutableVec2d()
    private var deltaScroll = 0.0

    private val ptrPos = MutableVec2d()
    private val ptrPosInitial = MutableVec2d(Vec2d.ZERO)

    private val panPlane = Plane()
    private val pointerHitStart = MutableVec3f()
    private val pointerHit = MutableVec3f()
    private val tmpVec1 = MutableVec3f()
    private val tmpVec2 = MutableVec3f()

    private val mouseTransform = MutableMat4d()
    private val mouseTransformInv = MutableMat4d()

    private val matrixTransform: MatrixTransformD
        get() = transform as MatrixTransformD

    var smoothness: Double = 0.0
        set(value) {
            field = value
            val stiffness = if (!value.isFuzzyZero()) { 50.0 / value } else { 0.0 }
            vertRotAnimator.stiffness = stiffness
            horiRotAnimator.stiffness = stiffness
            zoomAnimator.stiffness = stiffness
        }

    init {
        transform = MatrixTransformD()
        smoothness = 0.5
        panPlane.p.set(Vec3f.ZERO)
        panPlane.n.set(Vec3f.Y_AXIS)

        onUpdate += { (view, ctx) ->
            doCamTransform(view, ctx)
        }
    }

    fun setMouseRotation(yaw: Float, pitch: Float) = setMouseRotation(yaw.toDouble(), pitch.toDouble())

    fun setMouseRotation(yaw: Double, pitch: Double) {
        vertRotAnimator.set(yaw)
        horiRotAnimator.set(pitch)
        verticalRotation = yaw
        horizontalRotation = pitch
    }

    fun setMouseTranslation(x: Float, y: Float, z: Float) = setMouseTranslation(x.toDouble(), y.toDouble(), z.toDouble())

    fun setMouseTranslation(x: Double, y: Double, z: Double) {
        translation.set(x, y, z)
    }

    fun setZoom(newZoom: Float) = setZoom(newZoom.toDouble())

    fun setZoom(newZoom: Double, min: Double = minZoom, max: Double = maxZoom) {
        zoom = newZoom
        zoomAnimator.set(zoom)
        minZoom = min
        maxZoom = max
    }

    fun updateTransform() {
        translationBounds?.clampToBounds(translation)

        if (isKeepingStandardTransform) {
            mouseTransform.invert(mouseTransformInv)
            matrixTransform.mul(mouseTransformInv)
        }

        val z = zoomAnimator.actual
        val vr = vertRotAnimator.actual
        val hr = horiRotAnimator.actual
        mouseTransform.setIdentity()
        mouseTransform.translate(translation.x, translation.y, translation.z)
        mouseTransform.scale(z)
        mouseTransform.rotate(vr.deg, verticalAxis)
        mouseTransform.rotate(hr.deg, horizontalAxis)

        if (isKeepingStandardTransform) {
            matrixTransform.mul(mouseTransform)
        } else {
            matrixTransform.setMatrix(mouseTransform)
        }
    }

    private fun doCamTransform(view: RenderPass.View, ctx: KoolContext) {
        if (dragMethod == DragMethod.PAN && panMethod.computePanPoint(pointerHit, view, ptrPos, ctx)) {
            if (dragStart) {
                dragStart = false
                pointerHitStart.set(pointerHit)

                // stop any ongoing smooth motion, as we start a new one
                stopSmoothMotion()

            } else {
                val s = (1 - smoothness).clamp(0.1, 1.0).toFloat()
                tmpVec1.set(pointerHitStart).subtract(pointerHit).mul(s)
                parent?.toLocalCoords(tmpVec1, 0f)

                // limit panning speed
                val tLen = tmpVec1.length()
                if (tLen > view.camera.globalRange * 0.5f) {
                    tmpVec1.mul(view.camera.globalRange * 0.5f / tLen)
                }

                translation.add(tmpVec1)
            }
        } else {
            pointerHit.set(view.camera.globalLookAt)
        }

        if (!deltaScroll.isFuzzyZero()) {
            zoom *= 1f - deltaScroll / 10f
            deltaScroll = 0.0
        }

        if (dragMethod == DragMethod.ROTATE) {
            if(ptrPosInitial != Vec2d.ZERO){
                verticalRotation -= ( ptrPosInitial.x-ptrPos.x) / 3 * if (invertRotX) { -1f } else { 1f }
                horizontalRotation -= ( ptrPosInitial.y-ptrPos.y) / 3 * if (invertRotY) { -1f } else { 1f }
                ptrPosInitial.set(Vec2d.ZERO)
            }else{
                verticalRotation -= deltaPos.x / 3 * if (invertRotX) { -1f } else { 1f }
                horizontalRotation -= deltaPos.y / 3 * if (invertRotY) { -1f } else { 1f }
            }
            horizontalRotation = horizontalRotation.clamp(minHorizontalRot, maxHorizontalRot)
            deltaPos.set(Vec2d.ZERO)
        }

        vertRotAnimator.desired = verticalRotation
        horiRotAnimator.desired = horizontalRotation
        zoomAnimator.desired = zoom

        val oldZ = zoomAnimator.actual
        val z = zoomAnimator.animate(Time.deltaT)
        if (!isFuzzyEqual(oldZ, z)
                && zoomMethod == ZoomMethod.ZOOM_TRANSLATE
                && panMethod.computePanPoint(pointerHit, view, ptrPos, ctx)) {
            computeZoomTranslationPerspective(view.camera, oldZ, z)
        }

        vertRotAnimator.animate(Time.deltaT)
        horiRotAnimator.animate(Time.deltaT)
        updateTransform()
    }

    /**
     * Computes the required camera translation so that the camera zooms to the point under the pointer (only works
     * with perspective cameras)
     */
    protected open fun computeZoomTranslationPerspective(camera: Camera, oldZoom: Double, newZoom: Double) {
        // tmpVec1 = zoomed pos on pointer ray
        val s = (newZoom / oldZoom).toFloat()
        camera.globalPos.subtract(pointerHit, tmpVec1).mul(s).add(pointerHit)
        // tmpVec2 = zoomed pos on view center ray
        camera.globalPos.subtract(camera.globalLookAt, tmpVec2).mul(s)
            .add(camera.globalLookAt)
        tmpVec1.subtract(tmpVec2)
        parent?.toLocalCoords(tmpVec1, 0f)
        translation.add(tmpVec1)
    }

    private fun stopSmoothMotion() {
        vertRotAnimator.set(vertRotAnimator.actual)
        horiRotAnimator.set(horiRotAnimator.actual)
        zoomAnimator.set(zoomAnimator.actual)

        verticalRotation = vertRotAnimator.actual
        horizontalRotation = horiRotAnimator.actual
        zoom = zoomAnimator.actual
    }

    override fun handlePointer(pointerState: PointerState, ctx: KoolContext) {
        val dragPtr = pointerState.primaryPointer
        if (dragPtr.isConsumed()) {
            deltaPos.set(Vec2d.ZERO)
            deltaScroll = 0.0
            return
        }

        if (dragPtr.buttonEventMask != 0 || dragPtr.buttonMask != prevButtonMask) {
            dragMethod = when {
                dragPtr.isLeftButtonDown -> leftDragMethod
                dragPtr.isRightButtonDown -> rightDragMethod
                dragPtr.isMiddleButtonDown -> middleDragMethod
                else -> DragMethod.NONE
            }
            dragStart = dragMethod != DragMethod.NONE

            if(dragMethod == DragMethod.ROTATE){
                ptrPosInitial.set(dragPtr.x, dragPtr.y)
            }
        }

        prevButtonMask = dragPtr.buttonMask
        ptrPos.set(dragPtr.x, dragPtr.y)
        deltaPos.set(dragPtr.deltaX, dragPtr.deltaY)
        deltaScroll = dragPtr.deltaScroll
        if (isConsumingPtr) {
            dragPtr.consume()
        }
    }

    private fun MutableVec3d.add(v: Vec3f) {
        x += v.x
        y += v.y
        z += v.z
    }

    enum class DragMethod {
        NONE,
        ROTATE,
        PAN
    }

    enum class ZoomMethod {
        ZOOM_CENTER,
        ZOOM_TRANSLATE
    }
}

abstract class PanBase {
    abstract fun computePanPoint(result: MutableVec3f, view: RenderPass.View, ptrPos: Vec2d, ctx: KoolContext): Boolean
}

class CameraOrthogonalPan : PanBase() {
    val panPlane = Plane()
    private val pointerRay = RayF()

    override fun computePanPoint(result: MutableVec3f, view: RenderPass.View, ptrPos: Vec2d, ctx: KoolContext): Boolean {
        panPlane.p.set(view.camera.globalLookAt)
        panPlane.n.set(view.camera.globalLookDir)
        return view.camera.computePickRay(pointerRay, ptrPos.x.toFloat(), ptrPos.y.toFloat(), view.viewport) &&
                panPlane.intersectionPoint(pointerRay, result)
    }
}

class FixedPlanePan(planeNormal: Vec3f) : PanBase() {
    val panPlane = Plane()
    private val pointerRay = RayF()

    init {
        panPlane.n.set(planeNormal)
    }

    override fun computePanPoint(result: MutableVec3f, view: RenderPass.View, ptrPos: Vec2d, ctx: KoolContext): Boolean {
        panPlane.p.set(view.camera.globalLookAt)
        return view.camera.computePickRay(pointerRay, ptrPos.x.toFloat(), ptrPos.y.toFloat(), view.viewport) &&
                panPlane.intersectionPoint(pointerRay, result)
    }
}

fun xPlanePan() = FixedPlanePan(Vec3f.X_AXIS)
fun yPlanePan() = FixedPlanePan(Vec3f.Y_AXIS)
fun zPlanePan() = FixedPlanePan(Vec3f.Z_AXIS)
