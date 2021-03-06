package de.fabmax.kool.physics

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.physics.geometry.PlaneGeometry
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.PerfTimer
import de.fabmax.kool.util.logW
import kotlin.math.min

expect class PhysicsWorld(gravity: Vec3f = Vec3f(0f, -9.81f, 0f), numWorkers: Int = 4) : CommonPhysicsWorld {
    var gravity: Vec3f
}

abstract class CommonPhysicsWorld : Releasable {
    var physicsTime = 0.0
    var physicsTimeDesired = 0.0

    var singleStepTime = 1f / 60f
    var isStepAsync = true
    var isStepInProgress = false
    var smartSubSteps = true
    private var smartSubStepLimit = 5

    private val perfTimer = PerfTimer()
    var cpuTime = 0f
        private set
    var timeFactor = 1f
        private set

    val onFixedUpdate = mutableListOf<(Float) -> Unit>()

    private val mutActors = mutableListOf<RigidActor>()
    val actors: List<RigidActor>
        get() = mutActors

    protected val triggerListeners = mutableMapOf<RigidActor, TriggerListenerContext>()

    private var registeredAtScene: Scene? = null
    private val onRenderSceneHook: (KoolContext) -> Unit = { ctx ->
        if (isStepInProgress) {
            fetchStepResults()
        }

        stepPhysics(ctx.deltaT)

        if (isStepAsync && isContinueStep(physicsTime, physicsTimeDesired + singleStepTime, singleStepTime)) {
            singleStepPhysics()
            physicsTime += singleStepTime
        }
    }

    open fun registerHandlers(scene: Scene) {
        unregisterHandlers()
        registeredAtScene = scene
        scene.onRenderScene += onRenderSceneHook
    }

    open fun unregisterHandlers() {
        registeredAtScene?.let { it.onRenderScene -= onRenderSceneHook }
    }

    fun registerTriggerListener(trigger: RigidActor, listener: TriggerListener) {
        if (!trigger.isTrigger) {
            logW { "Given trigger actor is not a trigger (isTrigger == false)" }
        }
        triggerListeners.getOrPut(trigger) { TriggerListenerContext() }.listeners += listener
    }

    fun unregisterTriggerListener(listener: TriggerListener) {
        triggerListeners.values.forEach { it.listeners -= listener }
        triggerListeners.keys.removeAll { k -> triggerListeners[k]?.listeners?.isEmpty() ?: false }
    }

    override fun release() {
        if (isStepInProgress) {
            fetchStepResults()
        }
        unregisterHandlers()
        clear(true)
    }

    open fun addActor(actor: RigidActor) {
        mutActors += actor
    }

    open fun removeActor(actor: RigidActor) {
        mutActors -= actor
    }

    fun clear(releaseActors: Boolean = true) {
        val removeActors = mutableListOf<RigidActor>().apply { this += actors }
        for (i in removeActors.lastIndex downTo 0) {
            removeActor(removeActors[i])
            if (releaseActors) {
                removeActors[i].release()
            }
        }
        triggerListeners.clear()
    }

    fun stepPhysics(timeStep: Float, maxSubSteps: Int = 5): Float {
        var steps = 0
        var stepLimit = if (smartSubSteps) min(smartSubStepLimit, maxSubSteps) else maxSubSteps

        physicsTimeDesired += timeStep
        while (isContinueStep(physicsTime, physicsTimeDesired, singleStepTime) && stepLimit > 0) {
            perfTimer.reset()

            singleStepPhysics()
            fetchStepResults()
            steps++

            val ms = perfTimer.takeMs().toFloat()
            cpuTime = cpuTime * 0.8f + ms * 0.2f

            physicsTime += singleStepTime
            if (isContinueStep(physicsTime, physicsTimeDesired, singleStepTime)) {
                stepLimit--
            }
        }

        if (stepLimit == 0) {
            physicsTime = physicsTimeDesired
            if (smartSubStepLimit > 1) {
                smartSubStepLimit--
            }
        } else if (smartSubStepLimit < maxSubSteps) {
            smartSubStepLimit++
        }

        val timeInc = singleStepTime * steps
        timeFactor = timeFactor * 0.9f + timeInc / timeStep * 0.1f

        return timeInc
    }

    private fun isContinueStep(physicsTime: Double, physicsTimeDesired: Double, step: Float): Boolean {
        return physicsTimeDesired - physicsTime > step * 0.5
    }

    protected open fun singleStepPhysics() {
        isStepInProgress = true
    }

    protected open fun fetchStepResults() {
        isStepInProgress = false
        for (i in mutActors.indices) {
            mutActors[i].fixedUpdate(singleStepTime)
        }
        for (i in onFixedUpdate.indices) {
            onFixedUpdate[i](singleStepTime)
        }
    }

    /**
     * Adds a static plane with y-axis as surface normal (i.e. xz-plane) at y = 0.
     */
    fun addDefaultGroundPlane(): RigidStatic {
        val groundPlane = RigidStatic()
        val shape = Shape(PlaneGeometry(), Material(0.5f, 0.5f, 0.2f))
        groundPlane.attachShape(shape)
        groundPlane.setRotation(0f, 0f, 90f)
        addActor(groundPlane)
        return groundPlane
    }

    protected class TriggerListenerContext {
        val listeners = mutableListOf<TriggerListener>()
        val actorEnterCounts = mutableMapOf<RigidActor, Int>()
    }
}