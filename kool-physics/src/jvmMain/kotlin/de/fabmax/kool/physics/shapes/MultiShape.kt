package de.fabmax.kool.physics.shapes

import com.bulletphysics.collision.shapes.CompoundShape
import de.fabmax.kool.physics.toBtTransform

actual class MultiShape actual constructor() : CommonMultiShape(), CollisionShape {

    private val mutShapes = mutableListOf<ChildShape>()
    override val children: List<ChildShape>
        get() = mutShapes

    override val btShape: CompoundShape = CompoundShape()

    actual constructor(childShapes: List<ChildShape>) : this() {
        childShapes.forEach { addShape(it) }
    }

    override fun addShape(childShape: ChildShape) {
        mutShapes += childShape
        btShape.addChildShape(childShape.transform.toBtTransform(), childShape.shape.btShape)
    }

    override fun removeShape(shape: CollisionShape) {
        mutShapes.removeAll { it.shape === shape }
        btShape.removeChildShape(shape.btShape)
    }
}