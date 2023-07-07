package de.fabmax.kool.editor.model

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.editor.components.TransformComponent
import de.fabmax.kool.editor.data.SceneNodeData
import de.fabmax.kool.editor.data.TransformComponentData
import de.fabmax.kool.editor.data.TransformData
import de.fabmax.kool.scene.Node

class SceneNodeModel(nodeData: SceneNodeData, val parent: EditorNodeModel, val scene: SceneModel) : EditorNodeModel(nodeData) {

    override val drawNode: Node
        get() = created ?: throw IllegalStateException("Node was not yet created")

    private var created: Node? = null
    override val isCreated: Boolean
        get() = created != null

    val transform = getOrPutComponent { TransformComponent(TransformComponentData(TransformData.IDENTITY)) }

    init {
        nameState.onChange { created?.name = it }
    }

    override suspend fun createComponents() {
        super.createComponents()
        if (created == null) {
            created = Node(name)
        }
    }

    override fun addChild(child: SceneNodeModel) {
        nodeData.childNodeIds += child.nodeId
        drawNode.addNode(child.drawNode)
    }

    override fun removeChild(child: SceneNodeModel) {
        nodeData.childNodeIds -= child.nodeId
        drawNode.removeNode(child.drawNode)
    }

    fun disposeAndClearCreatedNode() {
        created?.let {
            it.dispose(KoolSystem.requireContext())
            it.parent?.removeNode(it)
        }
        created = null
    }

    fun setContentNode(newNode: Node) {
        created?.let {
            it.parent?.let { parent ->
                val ndIdx = parent.children.indexOf(it)
                parent.removeNode(it)
                parent.addNode(newNode, ndIdx)
            }
            scene.nodesToNodeModels -= it
            it.dispose(KoolSystem.requireContext())

            newNode.onUpdate += it.onUpdate
            it.onUpdate.clear()
        }
        transform.transformState.value.toTransform(newNode.transform)
        newNode.name = nodeData.name
        created = newNode
        scene.nodesToNodeModels[newNode] = this
    }
}