package de.fabmax.kool.physics.joints

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.util.BaseReleasable
import physx.PxConstraintFlagEnum
import physx.PxJoint
import physx.constraintFlags

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class JointHolder(val px: PxJoint)

abstract class JointImpl(frameA: Mat4f, frameB: Mat4f) : BaseReleasable(), Joint {
    override val frameA = Mat4f(frameA)
    override val frameB = Mat4f(frameB)

    abstract val pxJoint: PxJoint

    override val joint: JointHolder by lazy { JointHolder(pxJoint) }

    override val isBroken: Boolean
        get() = pxJoint.constraintFlags.isSet(PxConstraintFlagEnum.eBROKEN)

    override var debugVisualize: Boolean = false
        set(value) = if (value) {
            pxJoint.constraintFlags.raise(PxConstraintFlagEnum.eVISUALIZATION)
        } else {
            pxJoint.constraintFlags.clear(PxConstraintFlagEnum.eVISUALIZATION)
        }

    override fun setBreakForce(force: Float, torque: Float) = pxJoint.setBreakForce(force, torque)

    override fun release() {
        super.release()
        pxJoint.release()
    }
}