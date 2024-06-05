package de.fabmax.kool.editor.components

import de.fabmax.kool.editor.api.AppState
import de.fabmax.kool.editor.api.AssetReference
import de.fabmax.kool.editor.api.EditorProject
import de.fabmax.kool.editor.api.GameEntity
import de.fabmax.kool.editor.data.EntityId
import de.fabmax.kool.editor.data.MapAttribute
import de.fabmax.kool.editor.data.MaterialComponentData
import de.fabmax.kool.editor.data.MaterialData
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.launchOnMainThread

class MaterialComponent(
    gameEntity: GameEntity,
    componentData: MaterialComponentData = MaterialComponentData(EntityId(-1L))
) : GameEntityDataComponent<MaterialComponentData>(gameEntity, componentData) {

    val materialState = mutableStateOf<MaterialData?>(null).onChange { mat ->
        collectRequiredAssets(mat)
        if (AppState.isEditMode) {
            componentData.materialId = mat?.id ?: EntityId( -1)
        }
        if (isApplied) {
            launchOnMainThread {
                gameEntity.getComponents<UpdateMaterialComponent>().forEach { it.updateMaterial(mat) }
            }
        }
    }

    val materialData: MaterialData?
        get() = materialState.value

    init {
        materialState.set(project.materialsById[componentData.materialId])
    }

    fun isHoldingMaterial(material: MaterialData?): Boolean {
        return material?.id == materialData?.id
    }

    private fun collectRequiredAssets(material: MaterialData?) {
        requiredAssets.clear()
        if (material == null) {
            return
        }

        material.shaderData.collectAttributes().filterIsInstance<MapAttribute>().forEach { matMap ->
            requiredAssets += AssetReference.Texture(matMap.mapPath)
        }
    }
}

interface UpdateMaterialComponent {
    fun updateMaterial(material: MaterialData?)
}

fun EditorProject.updateMaterial(material: MaterialData) {
    createdScenes.values.forEach { scene ->
        scene.getAllComponents<UpdateMaterialComponent>().forEach {
            it.updateMaterial(material)
        }
    }
}
