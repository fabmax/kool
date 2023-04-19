package de.fabmax.kool.editor.model

import kotlinx.serialization.Serializable

@Serializable
data class MCommonNodeProperties(
    val hierarchyPath: MutableList<String>,
    val transform: MTransform
) {
    val name: String get() = hierarchyPath.last()
}