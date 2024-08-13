package de.fabmax.kool.editor.overlays

import de.fabmax.kool.editor.api.CachedSceneComponents
import de.fabmax.kool.editor.api.EditorScene
import de.fabmax.kool.editor.api.GameEntity
import de.fabmax.kool.editor.components.JointComponent
import de.fabmax.kool.editor.components.PhysicsComponent
import de.fabmax.kool.editor.data.JointData
import de.fabmax.kool.math.AngleF
import de.fabmax.kool.math.RayTest
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.physics.joints.D6JointMotion
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.MeshRayTest
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import kotlin.math.roundToInt

class PhysicsObjectsOverlay : Node("Physics objects overlay"), EditorOverlay {

    private var physicsComponentCache: CachedSceneComponents<PhysicsComponent>? = null
    private val physicsComponents: List<PhysicsComponent> get() = physicsComponentCache?.getComponents() ?: emptyList()

    private val joints = mutableListOf<JointInstance>()

    private val centerInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val linearXInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val linearYInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val linearZInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val angularXInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val angularYInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))
    private val angularZInstances = MeshInstanceList(listOf(Attribute.INSTANCE_MODEL_MAT, Attribute.INSTANCE_COLOR))

    private val jointCenterMesh = Mesh(meshAttrs, centerInstances, "centers").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            icoSphere {
                steps = 2
                radius = 0.05f
            }
        }
    }

    private val linearXJointMesh = Mesh(meshAttrs, linearXInstances, "linear-x").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            rotate(90f.deg, Vec3f.Z_AXIS)
            prismaticArrow()
        }
    }

    private val linearYJointMesh = Mesh(meshAttrs, linearYInstances, "linear-y").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            prismaticArrow()
        }
    }

    private val linearZJointMesh = Mesh(meshAttrs, linearZInstances, "linear-z").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            rotate(90f.deg, Vec3f.X_AXIS)
            prismaticArrow()
        }
    }

    private val angularXJointMesh = Mesh(meshAttrs, angularXInstances, "angular-x").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            withTransform {
                rotate(90f.deg, Vec3f.Z_AXIS)
                translate(0f, 0.3f, 0f)
                cylinder {
                    radius = 0.015f
                    height = 0.4f
                }
                translate(0f, -0.6f, 0f)
                cylinder {
                    radius = 0.015f
                    height = 0.4f
                }
            }

            rotationArrow(270f.deg)
        }
    }

    private val angularYJointMesh = Mesh(meshAttrs, angularYInstances, "angular-y").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            rotate(90f.deg, Vec3f.NEG_Z_AXIS)
            rotationArrow(120f.deg)
        }
    }

    private val angularZJointMesh = Mesh(meshAttrs, angularZInstances, "angular-z").apply {
        isCastingShadow = false
        rayTest = MeshRayTest.geometryTest(this)
        shader = objectShader
        generate {
            rotate(90f.deg, Vec3f.NEG_Z_AXIS)
            rotate(90f.deg, Vec3f.Y_AXIS)
            rotationArrow(120f.deg)
        }
    }

    init {
        addNode(jointCenterMesh)
        addNode(angularXJointMesh)
        addNode(angularYJointMesh)
        addNode(angularZJointMesh)
        addNode(linearXJointMesh)
        addNode(linearYJointMesh)
        addNode(linearZJointMesh)

        onUpdate {
            updateOverlayInstances()
        }
    }

    private fun updateOverlayInstances() {
        if (physicsComponentCache?.isOutdated == true) {
            joints.clear()
            joints += physicsComponents
                .filterIsInstance<JointComponent>()
                .map { JointInstance(it) }
        }

        centerInstances.addInstances(joints, Color.WHITE) { true }
        linearXInstances.addInstances(joints, MdColor.RED.toLinear()) { it.isLinearX }
        linearYInstances.addInstances(joints, MdColor.LIGHT_GREEN.toLinear()) { it.isLinearY }
        linearZInstances.addInstances(joints, MdColor.BLUE.toLinear()) { it.isLinearZ }
        angularXInstances.addInstances(joints, MdColor.PURPLE.toLinear()) { it.isAngularX }
        angularYInstances.addInstances(joints, MdColor.LIME.toLinear()) { it.isAngularY }
        angularZInstances.addInstances(joints, MdColor.CYAN.toLinear()) { it.isAngularZ }
    }

    private fun MeshInstanceList.addInstances(objs: List<JointInstance>, color: Color, filter: (JointInstance) -> Boolean) {
        clear()
        addInstancesUpTo(objs.size) { buf ->
            var addCount = 0
            for (i in objs.indices) {
                val j = objs[i]
                if (j.gameEntity.isVisible && filter(j)) {
                    j.addInstance(buf, color)
                    addCount++
                }
            }
            addCount
        }
    }

    private fun MeshBuilder.prismaticArrow() {
        withTransform {
            translate(0f, 0.3f, 0f)
            cylinder {
                radius = 0.015f
                height = 0.4f
            }
            translate(0f, -0.6f, 0f)
            cylinder {
                radius = 0.015f
                height = 0.4f
            }
        }

        withTransform {
            translate(0f, 0.5f, 0f)
            cylinder {
                topRadius = 0f
                topFill = false
                bottomRadius = 0.05f
                height = 0.1f
            }
            translate(0f, -1f, 0f)
            cylinder {
                topRadius = 0.05f
                bottomFill = false
                bottomRadius = 0f
                height = 0.1f
            }
        }
    }

    private fun MeshBuilder.rotationArrow(sweep: AngleF) {
        val p = profile {
            circleShape(0.015f, 8)
        }
        val steps = (sweep.deg / 8).roundToInt()
        for (i in 0..steps) {
            withTransform {
                rotate(sweep * (i / steps.toFloat()) - sweep * 0.5f, Vec3f.X_AXIS)
                translate(0f, 0.3f, 0f)
                p.sample()
            }
        }
        withTransform {
            rotate(sweep * -0.5f, Vec3f.X_AXIS)
            translate(0f, 0.3f, 0f)
            rotate(90f.deg, Vec3f.NEG_X_AXIS)
            cylinder {
                topRadius = 0f
                topFill = false
                bottomRadius = 0.05f
                height = 0.1f
            }
        }
        withTransform {
            rotate(sweep * 0.5f, Vec3f.X_AXIS)
            translate(0f, 0.3f, 0f)
            rotate(90f.deg, Vec3f.X_AXIS)
            cylinder {
                topRadius = 0f
                topFill = false
                bottomRadius = 0.05f
                height = 0.1f
            }
        }
    }

    override fun onEditorSceneChanged(scene: EditorScene) {
        physicsComponentCache = CachedSceneComponents(scene, PhysicsComponent::class)
    }

    override fun pick(rayTest: RayTest): GameEntity? {
        var closest: GameEntity? = null
        joints.forEach {
            if (it.rayTest(rayTest)) {
                closest = it.gameEntity
            }
        }
        return closest
    }

    companion object {
        private val meshAttrs = listOf(Attribute.POSITIONS)
        private val objectShader = KslUnlitShader {
            pipeline {
                depthTest = DepthCompareOp.ALWAYS
                isWriteDepth = false
            }
            vertices { isInstanced = true }
            color { instanceColor() }
            colorSpaceConversion = ColorSpaceConversion.LinearToSrgb()
        }
    }

    private val JointData.isLinearX: Boolean get() =
        this is JointData.Prismatic || this is JointData.Distance || (this is JointData.D6 && linearMotionX != D6JointMotion.Locked)
    private val JointData.isLinearY: Boolean get() =
        this is JointData.Distance || (this is JointData.D6 && linearMotionY != D6JointMotion.Locked)
    private val JointData.isLinearZ: Boolean get() =
        this is JointData.Distance || (this is JointData.D6 && linearMotionZ != D6JointMotion.Locked)
    private val JointData.isAngularX: Boolean get() =
        this is JointData.Revolute || this is JointData.Spherical || (this is JointData.D6 && angularMotionX != D6JointMotion.Locked)
    private val JointData.isAngularY: Boolean get() =
        this is JointData.Spherical || (this is JointData.D6 && angularMotionY != D6JointMotion.Locked)
    private val JointData.isAngularZ: Boolean get() =
        this is JointData.Spherical || (this is JointData.D6 && angularMotionZ != D6JointMotion.Locked)

    private inner class JointInstance(val jointComponent: JointComponent) : OverlayObject(jointComponent.gameEntity) {
        override val color: Color = Color.WHITE

        val isLinearX: Boolean get() = jointComponent.data.jointData.isLinearX
        val isLinearY: Boolean get() = jointComponent.data.jointData.isLinearY
        val isLinearZ: Boolean get() = jointComponent.data.jointData.isLinearZ
        val isAngularX: Boolean get() = jointComponent.data.jointData.isAngularX
        val isAngularY: Boolean get() = jointComponent.data.jointData.isAngularY
        val isAngularZ: Boolean get() = jointComponent.data.jointData.isAngularZ

        fun rayTest(rayTest: RayTest): Boolean {
            var isHit = rayTest(rayTest, jointCenterMesh)
            if (isLinearX) isHit = isHit || rayTest(rayTest, linearXJointMesh)
            if (isLinearY) isHit = isHit || rayTest(rayTest, linearYJointMesh)
            if (isLinearZ) isHit = isHit || rayTest(rayTest, linearZJointMesh)
            if (isAngularX) isHit = isHit || rayTest(rayTest, angularXJointMesh)
            if (isAngularY) isHit = isHit || rayTest(rayTest, angularYJointMesh)
            if (isAngularZ) isHit = isHit || rayTest(rayTest, angularZJointMesh)
            return isHit
        }
    }
}