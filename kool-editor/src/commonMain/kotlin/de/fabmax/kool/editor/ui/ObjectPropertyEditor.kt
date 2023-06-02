package de.fabmax.kool.editor.ui

import de.fabmax.kool.editor.EditorState
import de.fabmax.kool.editor.actions.AddComponentAction
import de.fabmax.kool.editor.actions.EditorActions
import de.fabmax.kool.editor.actions.SetTransformAction
import de.fabmax.kool.editor.data.ScriptComponentData
import de.fabmax.kool.editor.model.*
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.Mat3d
import de.fabmax.kool.math.Mat4d
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*

class ObjectPropertyEditor(ui: EditorUi) : EditorPanel("Object Properties", ui) {

    private val transformProperties = TransformProperties()

    private val tmpNodePos = MutableVec3d()
    private val tmpNodeRot = MutableVec3d()
    private val tmpNodeScale = MutableVec3d()
    private val tmpNodeRotMat = Mat3d()

    private val transformGizmo = NodeTransformGizmo(ui.editor)

    init {
        transformProperties.editHandlers += object : ValueEditHandler<Mat4d> {
            override fun onEdit(value: Mat4d) {
                val selectedNd = EditorState.selectedNode.value as? SceneNodeModel
                selectedNd?.node?.transform?.set(value)
            }

            override fun onEditEnd(startValue: Mat4d, endValue: Mat4d) {
                val selectedNd = EditorState.selectedNode.value as? SceneNodeModel
                if (selectedNd != null) {
                    applyTransformAction(selectedNd, startValue, endValue)
                }
            }
        }
        ui.editor.editorContent += transformGizmo
    }

    override val windowSurface: UiSurface = EditorPanelWindow {
        // clear gizmo transform object, will be set below if transform editor is available
        transformGizmo.setTransformObject(null)

        Column(Grow.Std, Grow.Std) {
            editorTitleBar(windowDockable)
            objectProperties(EditorState.selectedNode.use())
        }

        surface.onEachFrame {
            val selectedNd = EditorState.selectedNode.value as? SceneNodeModel
            if (selectedNd != null) {
                selectedNd.node.transform.getPosition(tmpNodePos)
                transformProperties.setPosition(tmpNodePos)
                selectedNd.node.transform.matrix.getRotation(tmpNodeRotMat)
                transformProperties.setRotation(tmpNodeRotMat.getEulerAngles(tmpNodeRot))
                selectedNd.node.transform.matrix.getScale(tmpNodeScale)
                transformProperties.setScale(tmpNodeScale)
            }
        }
    }

    private fun UiScope.objectProperties(selectedObject: EditorNodeModel?) = ScrollArea(
        containerModifier = { it.backgroundColor(null) },
        isScrollableHorizontal = false
    ) {
        modifier.width(Grow.Std)

        Column(Grow.Std, Grow.Std, scopeName = selectedObject?.name) {
            Row(width = Grow.Std, height = sizes.lineHeightLarger) {
                modifier
                    .padding(horizontal = sizes.gap)

                if (selectedObject == null) {
                    Text("Nothing selected") {
                        modifier
                            .width(Grow.Std)
                            .textAlignX(AlignmentX.Center)
                            .alignY(AlignmentY.Bottom)
                            .font(sizes.italicText)
                    }
                } else {
                    Text(selectedObject.name) {
                        modifier
                            .alignY(AlignmentY.Center)
                            .font(sizes.boldText)
                    }
                }
            }

            if (selectedObject == null) {
                return@Column
            }

            for (component in selectedObject.components.use()) {
                when (component) {
                    is MaterialComponent -> materialComponent(selectedObject, component)
                    is MeshComponent -> meshComponent(selectedObject, component)
                    is SceneBackgroundComponent -> sceneBackgroundComponent(selectedObject)
                    is ScriptComponent -> scriptComponent(component)
                    is TransformComponent -> transformComponent(selectedObject)
                }
            }

            addComponentSelector(selectedObject)
        }
    }

    private fun UiScope.materialComponent(nodeModel: EditorNodeModel, materialComponent: MaterialComponent) {
        (nodeModel as? SceneNodeModel)?.let {
            val editor = remember { MaterialEditor(nodeModel, materialComponent) }
            editor.materialComponent = materialComponent
            editor()
        }
    }

    private fun UiScope.meshComponent(nodeModel: EditorNodeModel, meshComponent: MeshComponent) {
        (nodeModel as? SceneNodeModel)?.let {
            meshTypeProperties(nodeModel, meshComponent)
        }
    }

    private fun UiScope.sceneBackgroundComponent(nodeModel: EditorNodeModel) {
        (nodeModel as? SceneModel)?.let {
            val editor = remember { SceneBackgroundEditor(it) }
            editor.sceneModel = it
            editor()
        }
    }

    private fun UiScope.scriptComponent(scriptComponent: ScriptComponent) {
        val title = remember {
            val simpleName = scriptComponent.scriptClassNameState.value
                .replaceBeforeLast('.', "")
                .removePrefix(".")
            ScriptEditor.camelCaseToWords(simpleName)
        }
        collapsapsablePanel(
            title = title,
            scopeName = scriptComponent.componentData.scriptClassName
        ) {
            val scriptEditor = remember { ScriptEditor(scriptComponent) }
            scriptEditor()
        }
    }

    private fun UiScope.transformComponent(nodeModel: EditorNodeModel) {
        (nodeModel as? SceneNodeModel)?.let {
            transformGizmo.setTransformObject(it)
            transformEditor(transformProperties)
        }
    }

    private fun UiScope.addComponentSelector(nodeModel: EditorNodeModel) {
        if (nodeModel !is SceneNodeModel) {
            // currently there are no useful components we can add to a scene...
            return
        }

        var isScriptPopupOpen by remember(false)

        // make this some kind of combobox / menu, once there are more options
        val button = Button("Add script") {
            defaultButtonStyle()
            modifier
                .width(sizes.baseSize * 5)
                .margin(top = sizes.gap)
                .alignX(AlignmentX.Center)
                .onClick { isScriptPopupOpen = true }
        }

        if (isScriptPopupOpen) {
            Popup(button.uiNode.leftPx, button.uiNode.bottomPx) {
                Column {
                    modifier
                        .background(RoundRectBackground(colors.background, sizes.smallGap))
                        .border(RoundRectBorder(colors.secondaryVariant, sizes.smallGap, sizes.borderWidth))
                        .padding(sizes.smallGap)

                    var hoverIdx by remember(-1)
                    val scriptClasses = EditorState.loadedApp.use()?.scriptClasses?.values ?: emptyList()
                    scriptClasses.forEachIndexed { i, scriptClass ->
                        scriptClass.klass.qualifiedName?.let { scriptFqn ->
                            Text(scriptFqn) {
                                modifier
                                    .size(Grow.Std, sizes.lineHeight)
                                    .padding(horizontal = sizes.gap)
                                    .onEnter { hoverIdx = i }
                                    .onExit { hoverIdx = -1 }
                                    .onClick {
                                        isScriptPopupOpen = false
                                        EditorActions.applyAction(
                                            AddComponentAction(
                                                nodeModel,
                                                ScriptComponent(ScriptComponentData(scriptFqn))
                                            )
                                        )
                                    }

                                if (i == hoverIdx) {
                                    modifier.background(RoundRectBackground(colors.hoverBg, sizes.smallGap))
                                }
                            }
                        }
                    }
                }

                // close popup menu on any button event outside popup menu
                surface.onEachFrame {
                    val ptr = PointerInput.primaryPointer
                    if (ptr.isAnyButtonEvent && !uiNode.isInBounds(Vec2f(ptr.x.toFloat(), ptr.y.toFloat()))) {
                        isScriptPopupOpen = false
                    }
                }
            }
        }
    }

    companion object {
        fun applyTransformAction(nodeModel: SceneNodeModel, oldTransform: Mat4d, newTransform: Mat4d) {
            val setTransform = SetTransformAction(
                editedNodeModel = nodeModel,
                oldTransform = oldTransform,
                newTransform = newTransform
            )
            EditorActions.applyAction(setTransform)
        }
    }
}