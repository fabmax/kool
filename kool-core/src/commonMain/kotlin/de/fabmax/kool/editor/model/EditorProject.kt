package de.fabmax.kool.editor.model

import de.fabmax.kool.Assets
import de.fabmax.kool.editor.data.MaterialData
import de.fabmax.kool.editor.data.PbrShaderData
import de.fabmax.kool.editor.data.ProjectData
import de.fabmax.kool.editor.data.SceneNodeData
import de.fabmax.kool.modules.ui2.mutableStateListOf
import de.fabmax.kool.util.logE
import kotlinx.serialization.json.Json

class EditorProject(val projectData: ProjectData) {

    val entities = mutableListOf<NodeModel>()

    private val _sceneNodeData = projectData.sceneNodes.associateBy { it.nodeId }.toMutableMap()
    val sceneNodeData: Map<Long, SceneNodeData>
        get() = _sceneNodeData

    private val _materialsById = projectData.materials.associateBy { it.id }.toMutableMap()
    val materialsById: Map<Long, MaterialData>
        get() = _materialsById
    val materials = mutableStateListOf<MaterialData>().apply {
        addAll(projectData.materials)
        sortBy { it.name }
    }

    private val created: MutableMap<Long, SceneModel> = mutableMapOf()

    private fun checkProjectModelConsistency() {
        val nodeMap = projectData.sceneNodes.associateBy { it.nodeId }
        val referencedNodeIds = mutableSetOf<Long>()

        fun collectChildNodeIds(node: SceneNodeData) {
            node.childNodeIds.forEach { childId ->
                val child = nodeMap[childId]
                if (child == null) {
                    logE { "Node \"${node.name}\" references non-existing child node $childId" }
                } else {
                    referencedNodeIds += childId
                    collectChildNodeIds(child)
                }
            }
        }

        projectData.sceneNodeIds.forEach { sceneId ->
            val scene = nodeMap[sceneId]
            if (scene == null) {
                logE { "Project references non-existing scene $sceneId" }
            } else {
                referencedNodeIds += sceneId
                collectChildNodeIds(scene)
            }
        }

        val unreferencedIds = nodeMap.keys - referencedNodeIds
        if (unreferencedIds.isNotEmpty()) {
            logE { "Project contains unreferenced nodes: ${unreferencedIds.map { "$it: ${nodeMap[it]?.name}" }}" }
        }
    }

    suspend fun create() {
        checkProjectModelConsistency()
        projectData.sceneNodeIds.forEach { sceneNodeId ->
            val sceneData = sceneNodeData[sceneNodeId]
            if (sceneData != null) {
                val sceneModel = created.getOrPut(sceneNodeId) { SceneModel(sceneData, this) }
                sceneModel.createScene()
            }
        }
    }

    fun getCreatedScenes(): List<SceneModel> = created.values.toList()

    fun nextId(): Long {
        return projectData.nextId++
    }

    fun addSceneNodeData(data: SceneNodeData) {
        projectData.sceneNodes += data
        _sceneNodeData[data.nodeId] = data
    }

    fun removeSceneNodeData(data: SceneNodeData) {
        projectData.sceneNodes -= data
        _sceneNodeData -= data.nodeId
    }

    fun createNewMaterial(): MaterialData {
        val id = nextId()
        val newMat = MaterialData(id, "Material-$id", PbrShaderData())
        addMaterial(newMat)
        return newMat
    }

    fun removeMaterial(material: MaterialData) {
        _materialsById -= material.id
        projectData.materials -= material
        materials -= material
    }

    fun addMaterial(material: MaterialData) {
        _materialsById[material.id] = material
        projectData.materials += material
        materials += material
        materials.sortBy { it.name }
    }

    inline fun <reified T: Any> getAllComponents(): List<T> {
        return entities.flatMap { it.components.filterIsInstance<T>() }
    }

    inline fun <reified T: Any> getComponentsFromEntities(predicate: (NodeModel) -> Boolean): List<T> {
        return entities.filter(predicate).flatMap { it.components.filterIsInstance<T>() }
    }

    inline fun <reified T: Any> getComponentsInScene(sceneModel: SceneModel): List<T> {
        return getComponentsFromEntities { it === sceneModel || (it is SceneNodeModel && it.sceneModel === sceneModel) }
    }

    companion object {
        suspend fun loadFromAssets(path: String = "kool-project.json"): EditorProject? {
            return try {
                val json = Assets.loadBlobAsset(path).toArray().decodeToString()
                val projectData = Json.Default.decodeFromString(ProjectData.serializer(), json)
                EditorProject(projectData)
            } catch (e: Exception) {
                null
            }
        }
    }
}
