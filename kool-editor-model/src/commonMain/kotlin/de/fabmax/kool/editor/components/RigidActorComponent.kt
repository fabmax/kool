package de.fabmax.kool.editor.components

import de.fabmax.kool.editor.api.AppAssets
import de.fabmax.kool.editor.api.GameEntity
import de.fabmax.kool.editor.data.*
import de.fabmax.kool.math.*
import de.fabmax.kool.physics.*
import de.fabmax.kool.physics.geometry.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.util.BufferedList
import de.fabmax.kool.util.launchOnMainThread
import de.fabmax.kool.util.logE

class RigidActorComponent(
    gameEntity: GameEntity,
    componentInfo: ComponentInfo<RigidActorComponentData> = ComponentInfo(RigidActorComponentData())
) :
    PhysicsComponent<RigidActorComponentData>(gameEntity, componentInfo),
    MeshComponent.ListenerComponent
{
    var rigidActor: RigidActor? = null
        private set

    private var geometry: List<CollisionGeometry> = emptyList()
    private var bodyShapes: List<ShapeData> = emptyList()

    val triggerListeners = BufferedList<TriggerListener>()
    private val proxyTriggerListener = object : TriggerListener {
        override fun onActorEntered(trigger: RigidActor, actor: RigidActor) {
            triggerListeners.updated().forEach {
                it.onActorEntered(trigger, actor)
            }
        }

        override fun onActorExited(trigger: RigidActor, actor: RigidActor) {
            triggerListeners.updated().forEach {
                it.onActorExited(trigger, actor)
            }
        }
    }

    override val actorTransform: TrsTransformF? get() = rigidActor?.transform

    init {
        dependsOn(MeshComponent::class, isOptional = true)
        dependsOn(ModelComponent::class, isOptional = true)

        data.shapes
            .filterIsInstance<ShapeData.Heightmap>()
            .filter { it.mapPath.isNotBlank() }
            .forEach { requiredAssets += it.toAssetReference() }
    }

    override fun onDataChanged(oldData: RigidActorComponentData, newData: RigidActorComponentData) {
        launchOnMainThread {
            updateRigidActor(newData)
        }
    }

    fun addTriggerListener(listener: TriggerListener) {
        triggerListeners += listener
    }

    fun removeTriggerListener(listener: TriggerListener) {
        triggerListeners += listener
    }

    override suspend fun applyComponent() {
        super.applyComponent()
        createRigidBody(data)
    }

    override fun destroyComponent() {
        rigidActor?.let {
            physicsWorld?.removeActor(it)
            it.release()
        }
        rigidActor = null
        geometry.forEach { it.release() }
        geometry = emptyList()
        super.destroyComponent()
    }

    private suspend fun updateRigidActor(actorData: RigidActorComponentData) {
        val actor = rigidActor
        val isActorOk = when (actorData.actorType) {
            RigidActorType.DYNAMIC -> actor is RigidDynamic && !actor.isKinematic
            RigidActorType.KINEMATIC -> actor is RigidDynamic && actor.isKinematic
            RigidActorType.STATIC -> actor is RigidStatic
        }

        if (!isActorOk || actorData.shapes != bodyShapes) {
            createRigidBody(actorData)

        } else if (actor is RigidDynamic) {
            actor.mass = actorData.mass.toFloat()
            actor.updateInertiaFromShapesAndMass()
            actor.characterControllerHitBehavior = actorData.characterControllerHitBehavior
        }

        actor?.apply {
            if (isTrigger != actorData.isTrigger) {
                isTrigger = actorData.isTrigger
                if (isTrigger) {
                    physicsWorld?.registerTriggerListener(this, proxyTriggerListener)
                } else {
                    physicsWorld?.unregisterTriggerListener(proxyTriggerListener)
                }
            }
        }
    }

    private suspend fun createRigidBody(actorData: RigidActorComponentData) {
        val physicsWorldComponent = getOrCreatePhysicsWorldComponent()
        val physicsWorld = physicsWorldComponent.physicsWorld
        if (physicsWorld == null) {
            logE { "Unable to create rigid body: parent physics world was not yet created" }
        }

        rigidActor?.let {
            physicsWorld?.removeActor(it)
            it.release()
        }
        geometry.forEach { it.release() }

        rigidActor = when (actorData.actorType) {
            RigidActorType.DYNAMIC -> RigidDynamic(actorData.mass.toFloat())
            RigidActorType.KINEMATIC -> RigidDynamic(actorData.mass.toFloat(), isKinematic = true)
            RigidActorType.STATIC -> RigidStatic()
        }

        scale.set(Vec3d.ONES)

        requiredAssets.clear()
        rigidActor?.apply {
            bodyShapes = actorData.shapes

            val meshComp = gameEntity.getComponent<MeshComponent>()
            val modelComp = gameEntity.getComponent<ModelComponent>()

            val shapes = when {
                bodyShapes.isNotEmpty() -> bodyShapes.mapNotNull { shape -> shape.makeCollisionGeometry() }
                meshComp != null -> meshComp.makeCollisionShapes()
                modelComp != null -> modelComp.makeCollisionShapes()
                else -> emptyList()
            }

            shapes.forEach { (shape, pose) -> attachShape(Shape(shape, localPose = pose)) }
            geometry = shapes.map { it.first }
            physicsWorld?.addActor(this)
        }

        updateRigidActor(actorData)
        setPhysicsTransformFromDrawNode()
    }

    private suspend fun MeshComponent.makeCollisionShapes(): List<Pair<CollisionGeometry, Mat4f>> {
        return data.shapes.mapNotNull { shape -> shape.makeCollisionGeometry(drawNode) }
    }

    private fun ModelComponent.makeCollisionShapes(): List<Pair<CollisionGeometry, Mat4f>> {
        val model = drawNode ?: return emptyList()

        model.transform.decompose(scale = scale)

        val collisionGeom = IndexedVertexList(Attribute.POSITIONS)
        val globalToModel = model.invModelMatD
        model.meshes.values.forEach { mesh ->
            val meshToModel = mesh.modelMatD * globalToModel
            collisionGeom.addGeometry(mesh.geometry) {
                meshToModel.transform(position, 1f)
            }
        }
        return listOf(collisionGeom.makeTriMeshGeometry(scale.toVec3f()) to Mat4f.IDENTITY)
    }

    private suspend fun ShapeData.makeCollisionGeometry(mesh: Mesh? = null): Pair<CollisionGeometry, Mat4f>? {
        return when (this) {
            is ShapeData.Box -> BoxGeometry(size.toVec3f()) to Mat4f.IDENTITY
            is ShapeData.Capsule -> CapsuleGeometry(length.toFloat(), radius.toFloat()) to Mat4f.IDENTITY
            is ShapeData.Cylinder -> CylinderGeometry(length.toFloat(), bottomRadius.toFloat()) to Mat4f.IDENTITY
            is ShapeData.Sphere -> SphereGeometry(radius.toFloat()) to Mat4f.IDENTITY
            is ShapeData.Heightmap -> loadHeightmapGeometry(this)?.let { it to Mat4f.IDENTITY }
            is ShapeData.Plane -> PlaneGeometry() to Mat4f.rotation(90f.deg, Vec3f.Z_AXIS)
            is ShapeData.Rect -> mesh?.let { it.geometry.makeTriMeshGeometry(Vec3f.ONES) to Mat4f.IDENTITY }
            is ShapeData.Custom -> null
        }
    }

    private fun IndexedVertexList.makeTriMeshGeometry(scale: Vec3f): TriangleMeshGeometry {
        return TriangleMeshGeometry(this, scale)
    }

    private suspend fun loadHeightmapGeometry(shapeData: ShapeData.Heightmap): CollisionGeometry? {
        if (shapeData.mapPath.isBlank()) {
            return null
        }
        val heightmapRef = shapeData.toAssetReference()
        requiredAssets += heightmapRef
        val heightmap = AppAssets.loadHeightmap(heightmapRef) ?: return null
        val heightField = HeightField(heightmap, shapeData.rowScale.toFloat(), shapeData.colScale.toFloat())
        return HeightFieldGeometry(heightField)
    }

    override fun applyPose(position: Vec3d, rotation: QuatD) {
        rigidActor?.apply {
            this.position = position.toVec3f()
            this.rotation = rotation.toQuatF()
        }
    }

    override suspend fun onMeshGeometryChanged(component: MeshComponent, newData: MeshComponentData) {
        if (data.shapes.isEmpty()) {
            createRigidBody(data)
        }
    }
}