package de.fabmax.kool.physics.joints

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.physics.MemoryStack
import de.fabmax.kool.physics.Physics
import de.fabmax.kool.physics.RigidActor
import de.fabmax.kool.physics.toPxTransform
import physx.*

actual enum class D6JointMotion(val pxVal: Int) {
    Free(PxD6MotionEnum.eFREE),
    Limited(PxD6MotionEnum.eLIMITED),
    Locked(PxD6MotionEnum.eLOCKED);

    companion object {
        fun fromPx(pxVal: Int) = when (pxVal) {
            PxD6MotionEnum.eFREE -> D6JointMotion.Free
            PxD6MotionEnum.eLIMITED -> D6JointMotion.Limited
            PxD6MotionEnum.eLOCKED -> D6JointMotion.Locked
            else -> throw RuntimeException()
        }
    }
}

actual class D6Joint actual constructor(
    actual val bodyA: RigidActor,
    actual val bodyB: RigidActor,
    posA: Mat4f,
    posB: Mat4f
) : Joint() {

    actual val frameA = Mat4f().set(posA)
    actual val frameB = Mat4f().set(posB)

    actual var projectionLinearTolerance: Float? = null
        set(value) = if (value != null) {
            pxJoint.projectionLinearTolerance = value
            pxJoint.constraintFlags.set(PxConstraintFlagEnum.ePROJECTION)
        } else {
            pxJoint.constraintFlags.clear(PxConstraintFlagEnum.ePROJECTION)
        }

    override val pxJoint: PxD6Joint

    init {
        Physics.checkIsLoaded()
        MemoryStack.stackPush().use { mem ->
            val frmA = frameA.toPxTransform(mem.createPxTransform())
            val frmB = frameB.toPxTransform(mem.createPxTransform())
            pxJoint = Physics.Px.D6JointCreate(Physics.physics, bodyA.pxRigidActor, frmA, bodyB.pxRigidActor, frmB)
        }
    }

    actual var motionX: D6JointMotion
        get() = D6JointMotion.fromPx(pxJoint.getMotion(PxD6AxisEnum.eX))
        set(value) = pxJoint.setMotion(PxD6AxisEnum.eX, value.pxVal)

    actual var motionY: D6JointMotion
        get() = D6JointMotion.fromPx(pxJoint.getMotion(PxD6AxisEnum.eY))
        set(value) = pxJoint.setMotion(PxD6AxisEnum.eY, value.pxVal)

    actual var motionZ: D6JointMotion
        get() = D6JointMotion.fromPx(pxJoint.getMotion(PxD6AxisEnum.eZ))
        set(value) = pxJoint.setMotion(PxD6AxisEnum.eZ, value.pxVal)

    actual fun setDistanceLimit(extend: Float, stiffness: Float, damping: Float) {
        pxJoint.setDistanceLimit(PxJointLinearLimit(extend, PxSpring(stiffness, damping)))
        //pxJoint.constraintFlags += PxD6JointDriveFlagEnum.eACCELERATION
        pxJoint.setConstraintFlag(PxD6JointDriveFlagEnum.eACCELERATION, true)
    }

    actual fun setXLinearLimit(lowerLimit: Float, upperLimit: Float, stiffness: Float, damping: Float) {
        pxJoint.setLinearLimit(PxD6AxisEnum.eX,
            PxJointLinearLimitPair(lowerLimit, upperLimit, PxSpring(stiffness, damping)))
        motionX = D6JointMotion.Limited
    }
    actual fun setYLinearLimit(lowerLimit: Float, upperLimit: Float, stiffness: Float, damping: Float) {
        pxJoint.setLinearLimit(PxD6AxisEnum.eY,
            PxJointLinearLimitPair(lowerLimit, upperLimit, PxSpring(stiffness, damping)))
        motionY = D6JointMotion.Limited
    }
    actual fun setZLinearLimit(lowerLimit: Float, upperLimit: Float, stiffness: Float, damping: Float) {
        pxJoint.setLinearLimit(PxD6AxisEnum.eZ,
            PxJointLinearLimitPair(lowerLimit, upperLimit, PxSpring(stiffness, damping)))
        motionZ = D6JointMotion.Limited
    }
}