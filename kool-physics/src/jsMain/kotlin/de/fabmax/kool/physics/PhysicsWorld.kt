package de.fabmax.kool.physics

import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.util.logE
import physx.*

actual class PhysicsWorld actual constructor(gravity: Vec3f, numWorkers: Int) : CommonPhysicsWorld(), Releasable {
    val scene: PxScene

    private val bufPxGravity = gravity.toPxVec3(PxVec3())
    private val bufGravity = MutableVec3f()
    actual var gravity: Vec3f
        get() = scene.gravity.toVec3f(bufGravity)
        set(value) {
            scene.gravity = value.toPxVec3(bufPxGravity)
        }

    private val pxActors = mutableMapOf<Int, RigidActor>()

    init {
        Physics.checkIsLoaded()

        val sceneDesc = PxSceneDesc(Physics.physics.tolerancesScale)
        sceneDesc.gravity = bufPxGravity
        // ignore numWorkers parameter and set numThreads to 0, since multi-threading is disabled for wasm
        sceneDesc.cpuDispatcher = Physics.Px.DefaultCpuDispatcherCreate(0)
        sceneDesc.filterShader = Physics.Px.DefaultFilterShader()
        sceneDesc.flags.set(PxSceneFlagEnum.eENABLE_CCD)
        sceneDesc.simulationEventCallback = simEventCallback()
        scene = Physics.physics.createScene(sceneDesc)
    }

    override fun singleStepPhysics() {
        super.singleStepPhysics()
        scene.simulate(singleStepTime)
    }

    override fun fetchStepResults() {
        scene.fetchResults(true)
        super.fetchStepResults()
    }

    override fun addActor(actor: RigidActor) {
        super.addActor(actor)
        scene.addActor(actor.pxRigidActor)
        pxActors[actor.pxRigidActor.address] = actor
    }

    override fun removeActor(actor: RigidActor) {
        super.removeActor(actor)
        scene.removeActor(actor.pxRigidActor)
        pxActors -= actor.pxRigidActor.address
    }

    override fun release() {
        super.release()
        scene.release()
        bufPxGravity.destroy()
    }

    private fun simEventCallback() = JavaSimulationEventCallback().apply {
        onConstraintBreak = { _, _ -> }
        onWake = { _, _ -> }
        onSleep = { _, _ -> }
        onContact = { _, _, _ -> }

        onTrigger = { pairs: PxTriggerPair, count: Int ->
            for (i in 0 until count) {
                val pair = Physics.TypeHelpers.getTriggerPairAt(pairs, i)
                val isEnter = pair.status == PxPairFlagEnum.eNOTIFY_TOUCH_FOUND
                val trigger = pxActors[pair.triggerActor.address]
                val actor = pxActors[pair.otherActor.address]
                if (trigger != null && actor != null) {
                    triggerListeners[trigger]?.apply {
                        var cnt = actorEnterCounts.getOrPut(actor) { 0 }
                        val shapeAddr = pair.otherShape.address
                        val shape = actor.shapes.find { it.pxShape?.address == shapeAddr }
                        if (shape == null) {
                            logE { "shape reference not found" }
                        }
                        if (isEnter) {
                            cnt++
                            if (cnt == 1) {
                                listeners.forEach { it.onActorEntered(trigger, actor) }
                            }
                            shape?.let { s -> listeners.forEach { it.onShapeEntered(trigger, actor, s) } }
                        } else {
                            cnt--
                            shape?.let { s -> listeners.forEach { it.onShapeExited(trigger, actor, s) } }
                            if (cnt == 0) {
                                listeners.forEach { it.onActorExited(trigger, actor) }
                            }
                        }
                        actorEnterCounts[actor] = cnt
                    }
                } else {
                    logE { "actor reference not found" }
                }
            }
        }
    }
}