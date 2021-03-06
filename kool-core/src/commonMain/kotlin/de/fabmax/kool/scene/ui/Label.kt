package de.fabmax.kool.scene.ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.util.*

/**
 * @author fabmax
 */

open class Label(name: String, root: UiRoot) : UiComponent(name, root) {

    var text = name
        set(value) {
            if (value != field) {
                field = value
                requestUiUpdate()
            }
        }

    var textAlignment = Gravity(Alignment.START, Alignment.CENTER)
        set(value) {
            if (value != field) {
                field = value
                requestUiUpdate()
            }
        }

    // fixme: updateUi() is not issued when custom values are set
    val font = ThemeOrCustomProp<Font?>(null)
    val textColor = ThemeOrCustomProp(Color.WHITE)

    override fun setThemeProps(ctx: KoolContext) {
        super.setThemeProps(ctx)
        font.setTheme(standardFont(ctx))
        textColor.setTheme(root.theme.foregroundColor)
    }

    override fun createThemeUi(ctx: KoolContext): ComponentUi {
        return root.theme.newLabelUi(this)
    }
}

open class LabelUi(val label: Label, private val baseUi: ComponentUi) : ComponentUi by baseUi {

    protected var font = label.font.prop
    protected var textColor = MutableColor()

    protected val geom = IndexedVertexList(UiShader.UI_MESH_ATTRIBS)
    protected val meshBuilder = MeshBuilder(geom)
    protected val mesh = Mesh(geom)
    protected val shader = UiShader()

    protected var textStartX = 0f
    protected var textWidth = 0f
    protected var textBaseline = 0f

    override fun updateComponentAlpha() {
        baseUi.updateComponentAlpha()
        shader.alpha = label.alpha
    }

    override fun createUi(ctx: KoolContext) {
        baseUi.createUi(ctx)
        mesh.shader = shader
        label += mesh
    }

    override fun updateUi(ctx: KoolContext) {
        baseUi.updateUi(ctx)

        if (!label.font.isUpdate && label.font.prop == null) {
            label.font.setCustom(Font.defaultFont(ctx))
        }
        if (label.font.isUpdate) {
//            label.font.prop?.dispose(ctx)
            font = label.font.apply()
            shader.font = font
        }

        label.setupBuilder(meshBuilder)
        updateTextColor()
        computeTextMetrics()
        renderText(ctx)
    }

    override fun onRender(ctx: KoolContext) {
//        shader.setDrawBounds(label.drawBounds)
        baseUi.onRender(ctx)
    }

    override fun dispose(ctx: KoolContext) {
        baseUi.dispose(ctx)
        label -= mesh
        mesh.dispose(ctx)
    }

    protected open fun computeTextMetrics() {
        textWidth = font?.textWidth(label.text) ?: 0f

        textStartX = when (label.textAlignment.xAlignment) {
            Alignment.START -> label.padding.left.toUnits(label.width, label.dpi)
            Alignment.CENTER -> (label.width - textWidth) / 2f
            Alignment.END -> label.width - textWidth - label.padding.right.toUnits(label.width, label.dpi)
        }

        textBaseline = when (label.textAlignment.yAlignment) {
            Alignment.START -> label.height - label.padding.top.toUnits(label.width, label.dpi) - (font?.normHeight ?: 0f)
            Alignment.CENTER -> (label.height - (font?.normHeight ?: 0f)) / 2f
            Alignment.END -> label.padding.bottom.toUnits(label.height, label.dpi)
        }
    }

    protected open fun renderText(ctx: KoolContext) {
        meshBuilder.color = textColor
        val fnt = font
        if (fnt != null) {
            meshBuilder.text(fnt) {
                origin.set(textStartX, textBaseline, label.dp(.1f))
                text = label.text
            }
        }
    }

    protected open fun updateTextColor() {
        textColor.set(label.textColor.apply())
    }
}
