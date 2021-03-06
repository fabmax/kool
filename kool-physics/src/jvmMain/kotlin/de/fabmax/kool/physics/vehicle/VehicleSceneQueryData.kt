package de.fabmax.kool.physics.vehicle

import de.fabmax.kool.physics.Physics
import de.fabmax.kool.physics.Releasable
import de.fabmax.kool.physics.createPxBatchQueryDesc
import org.lwjgl.system.MemoryStack
import physx.PxTopLevelFunctions
import physx.physics.PxBatchQuery
import physx.physics.PxBatchQueryPostFilterShader
import physx.physics.PxBatchQueryPreFilterShader
import physx.physics.PxScene
import physx.support.Vector_PxRaycastHit
import physx.support.Vector_PxRaycastQueryResult
import physx.support.Vector_PxSweepHit
import physx.support.Vector_PxSweepQueryResult

class VehicleSceneQueryData(maxNumVehicles: Int, numWheelsPerVehicle: Int,
                            maxNumHitPointsPerWheel: Int, numVehiclesInBatch: Int = 1,
                            val preFilterShader: PxBatchQueryPreFilterShader? = PxTopLevelFunctions.DefaultWheelSceneQueryPreFilterBlocking(),
                            val postFilterShader: PxBatchQueryPostFilterShader? = null) : Releasable {

    val numQueriesPerBatch = numVehiclesInBatch * numWheelsPerVehicle
    val numHitResultsPerQuery = maxNumHitPointsPerWheel

    val raycastResults: Vector_PxRaycastQueryResult
    val raycastHitBuffer: Vector_PxRaycastHit

    val sweepResults: Vector_PxSweepQueryResult
    val sweepHitBuffer: Vector_PxSweepHit

    init {
        Physics.checkIsLoaded()

        val maxNumWheels = maxNumVehicles * numWheelsPerVehicle
        val maxNumHitPoints = maxNumWheels * maxNumHitPointsPerWheel

        raycastResults = Vector_PxRaycastQueryResult(maxNumWheels)
        raycastHitBuffer = Vector_PxRaycastHit(maxNumHitPoints)
        sweepResults = Vector_PxSweepQueryResult(maxNumWheels)
        sweepHitBuffer = Vector_PxSweepHit(maxNumHitPoints)
    }

    fun setupBatchedSceneQuery(scene: PxScene): PxBatchQuery {
        return MemoryStack.stackPush().use { mem ->
            val maxNumHitResultsInBatch = numQueriesPerBatch * numHitResultsPerQuery
            val sqDesc = mem.createPxBatchQueryDesc(numQueriesPerBatch, numHitResultsPerQuery, 0)

            sqDesc.queryMemory.userRaycastResultBuffer = raycastResults.data()
            sqDesc.queryMemory.userRaycastTouchBuffer = raycastHitBuffer.data()
            sqDesc.queryMemory.raycastTouchBufferSize = maxNumHitResultsInBatch

            sqDesc.queryMemory.userSweepResultBuffer = sweepResults.data()
            sqDesc.queryMemory.userSweepTouchBuffer = sweepHitBuffer.data()
            sqDesc.queryMemory.sweepTouchBufferSize = maxNumHitResultsInBatch

            preFilterShader?.let { sqDesc.preFilterShader = it }
            postFilterShader?.let { sqDesc.postFilterShader = it }

            scene.createBatchQuery(sqDesc)
        }
    }

    override fun release() {
        raycastResults.destroy()
        raycastHitBuffer.destroy()
        sweepResults.destroy()
        sweepHitBuffer.destroy()
    }
}