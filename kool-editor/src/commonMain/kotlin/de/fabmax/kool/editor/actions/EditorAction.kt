package de.fabmax.kool.editor.actions

import de.fabmax.kool.editor.KoolEditor

interface EditorAction {
    fun doAction()
    fun undoAction()

    fun apply() {
        EditorActions.applyAction(this)
    }
}

fun refreshComponentViews() {
    KoolEditor.instance.ui.sceneBrowser.refreshSceneTree()
    KoolEditor.instance.sceneObjectsOverlay.updateOverlayObjects()
}
