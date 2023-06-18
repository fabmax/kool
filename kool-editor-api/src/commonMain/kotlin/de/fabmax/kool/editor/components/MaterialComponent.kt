package de.fabmax.kool.editor.components

import de.fabmax.kool.editor.api.AppState
import de.fabmax.kool.editor.data.MaterialComponentData
import de.fabmax.kool.editor.data.MaterialData
import de.fabmax.kool.editor.model.EditorNodeModel
import de.fabmax.kool.editor.model.EditorProject
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.launchOnMainThread

class MaterialComponent(override val componentData: MaterialComponentData) :
    SceneNodeComponent(),
    EditorDataComponent<MaterialComponentData>
{

    constructor(): this(MaterialComponentData(-1L))

    val materialState = mutableStateOf<MaterialData?>(null).onChange { mat ->
        if (AppState.isEditMode) {
            componentData.materialId = mat?.id ?: -1
        }
        if (isCreated) {
            launchOnMainThread {
                sceneNode.getComponents<UpdateMaterialComponent>().forEach { it.updateMaterial(mat) }
            }
        }
    }

    val materialData: MaterialData?
        get() = materialState.value

    fun isHoldingMaterial(material: MaterialData?): Boolean {
        return material?.id == materialData?.id
    }

    override suspend fun createComponent(nodeModel: EditorNodeModel) {
        super.createComponent(nodeModel)
        materialState.set(scene.project.materialsById[componentData.materialId])
    }

    override suspend fun initComponent(nodeModel: EditorNodeModel) {
        super.initComponent(nodeModel)
        materialState.set(scene.project.materialsById[componentData.materialId])
    }
}

interface UpdateMaterialComponent : EditorModelComponent {
    fun updateMaterial(material: MaterialData?)
}

fun EditorProject.updateMaterial(material: MaterialData) {
    getAllComponents<UpdateMaterialComponent>().forEach {
        it.updateMaterial(material)
    }
}
