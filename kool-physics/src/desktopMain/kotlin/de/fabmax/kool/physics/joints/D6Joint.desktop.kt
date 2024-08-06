package de.fabmax.kool.physics.joints

import de.fabmax.kool.math.AngleF
import de.fabmax.kool.math.PoseF
import de.fabmax.kool.physics.PhysicsImpl
import de.fabmax.kool.physics.RigidActor
import de.fabmax.kool.physics.createPxTransform
import de.fabmax.kool.physics.toPxTransform
import de.fabmax.kool.util.memStack
import org.lwjgl.system.MemoryStack
import physx.PxTopLevelFunctions
import physx.extensions.*

actual fun D6Joint(bodyA: RigidActor?, bodyB: RigidActor, frameA: PoseF, frameB: PoseF): D6Joint {
    return D6JointImpl(bodyA, bodyB, frameA, frameB)
}

class D6JointImpl(
    override val bodyA: RigidActor?,
    override val bodyB: RigidActor,
    frameA: PoseF,
    frameB: PoseF
) : JointImpl(frameA, frameB), D6Joint {

    override val joint: PxD6Joint

    init {
        MemoryStack.stackPush().use { mem ->
            val frmA = frameA.toPxTransform(mem.createPxTransform())
            val frmB = frameB.toPxTransform(mem.createPxTransform())
            joint = PxTopLevelFunctions.D6JointCreate(PhysicsImpl.physics, bodyA?.holder, frmA, bodyB.holder, frmB)
        }
    }

    override var motionX: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eX).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eX, value.toPxD6MotionEnum())

    override var motionY: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eY).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eY, value.toPxD6MotionEnum())

    override var motionZ: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eZ).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eZ, value.toPxD6MotionEnum())

    override var motionTwist: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eTWIST).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eTWIST, value.toPxD6MotionEnum())

    override var motionSwing1: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eSWING1).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eSWING1, value.toPxD6MotionEnum())

    override var motionSwing2: D6JointMotion
        get() = joint.getMotion(PxD6AxisEnum.eSWING2).toD6JointMotion()
        set(value) = joint.setMotion(PxD6AxisEnum.eSWING2, value.toPxD6MotionEnum())

    override fun setDistanceLimit(extend: Float, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointLinearLimit.createAt(this, MemoryStack::nmalloc, extend, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setDistanceLimit(limit)
        }
    }

    override fun setXLinearLimit(lowerLimit: Float, upperLimit: Float, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointLinearLimitPair.createAt(this, MemoryStack::nmalloc, lowerLimit, upperLimit, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setLinearLimit(PxD6AxisEnum.eX, limit)
            motionX = D6JointMotion.Limited
        }
    }

    override fun setYLinearLimit(lowerLimit: Float, upperLimit: Float, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointLinearLimitPair.createAt(this, MemoryStack::nmalloc, lowerLimit, upperLimit, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setLinearLimit(PxD6AxisEnum.eY, limit)
            motionY = D6JointMotion.Limited
        }
    }

    override fun setZLinearLimit(lowerLimit: Float, upperLimit: Float, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointLinearLimitPair.createAt(this, MemoryStack::nmalloc, lowerLimit, upperLimit, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setLinearLimit(PxD6AxisEnum.eZ, limit)
            motionZ = D6JointMotion.Limited
        }
    }

    override fun setTwistLimit(lowerLimit: AngleF, upperLimit: AngleF, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointAngularLimitPair.createAt(this, MemoryStack::nmalloc, lowerLimit.rad, upperLimit.rad, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setTwistLimit(limit)
            motionTwist = D6JointMotion.Limited
        }
    }

    override fun setSwingLimit(yLimit: AngleF, zLimit: AngleF, limitBehavior: LimitBehavior) {
        memStack {
            val spring = PxSpring.createAt(this, MemoryStack::nmalloc, limitBehavior.stiffness, limitBehavior.damping)
            val limit = PxJointLimitCone.createAt(this, MemoryStack::nmalloc, yLimit.rad, zLimit.rad, spring)
            limit.restitution = limitBehavior.restitution
            limit.bounceThreshold = limitBehavior.bounceThreshold
            joint.setSwingLimit(limit)
            motionSwing1 = D6JointMotion.Limited
            motionSwing2 = D6JointMotion.Limited
        }
    }

    companion object {
        private fun PxD6MotionEnum.toD6JointMotion(): D6JointMotion = when (this) {
            PxD6MotionEnum.eFREE -> D6JointMotion.Free
            PxD6MotionEnum.eLIMITED -> D6JointMotion.Limited
            PxD6MotionEnum.eLOCKED -> D6JointMotion.Locked
        }

        private fun D6JointMotion.toPxD6MotionEnum(): PxD6MotionEnum = when (this) {
            D6JointMotion.Free -> PxD6MotionEnum.eFREE
            D6JointMotion.Limited -> PxD6MotionEnum.eLIMITED
            D6JointMotion.Locked -> PxD6MotionEnum.eLOCKED
        }
    }
}