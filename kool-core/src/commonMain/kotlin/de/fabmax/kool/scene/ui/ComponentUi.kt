package de.fabmax.kool.scene.ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.pipelineConfig
import de.fabmax.kool.pipeline.shading.ModeledShader
import de.fabmax.kool.scene.CullMethod
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshData
import de.fabmax.kool.shading.*
import de.fabmax.kool.util.BoundingBox
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Disposable
import de.fabmax.kool.util.MeshBuilder

interface ComponentUi : Disposable {

    fun updateComponentAlpha() { }

    fun createUi(ctx: KoolContext) { }

    fun updateUi(ctx: KoolContext) { }

    fun onRender(ctx: KoolContext) { }

    override fun dispose(ctx: KoolContext) { }
}

open class BlankComponentUi : ComponentUi

open class SimpleComponentUi(val component: UiComponent) : ComponentUi {

    protected var shader: BasicShader? = null
    protected val meshData = MeshData(Attribute.POSITIONS, Attribute.NORMALS, Attribute.COLORS, Attribute.TEXTURE_COORDS)
    protected val meshBuilder = MeshBuilder(meshData)
    protected val mesh = Mesh(meshData)

    val color: ThemeOrCustomProp<Color> = ThemeOrCustomProp(Color.BLACK.withAlpha(0.5f))

    override fun updateComponentAlpha() {
//        shader?.alpha = component.alpha
    }

    override fun createUi(ctx: KoolContext) {
        color.setTheme(component.root.theme.backgroundColor).apply()
//        shader = createShader(ctx)
//        shader?.staticColor?.set(color.prop)
//        mesh.shader = shader

        mesh.pipelineConfig {
            cullMethod = CullMethod.NO_CULLING
            shaderLoader = ModeledShader.vertexColor()
        }

        component.addNode(mesh, 0)
    }

    override fun dispose(ctx: KoolContext) {
        component -= mesh
        mesh.dispose(ctx)
    }

    override fun updateUi(ctx: KoolContext) {
        color.setTheme(component.root.theme.backgroundColor).apply()
//        shader?.staticColor?.set(color.prop)

        component.setupBuilder(meshBuilder)
        meshBuilder.color = color.prop
        meshBuilder.rect {
            size.set(component.width, component.height)
            fullTexCoords()
        }
    }

    protected open fun createShader(ctx: KoolContext): BasicShader {
        return basicShader {
            lightModel = component.root.shaderLightModel
            colorModel = ColorModel.STATIC_COLOR
            isAlpha = true
            clipMethod = LocalPlaneClip(6)
        }
    }

    override fun onRender(ctx: KoolContext) {
//        shader?.setDrawBounds(component.drawBounds)
    }
}

open class BlurredComponentUi(component: UiComponent) : SimpleComponentUi(component) {
    override fun createShader(ctx: KoolContext): BasicShader {
        return blurShader {
            lightModel = component.root.shaderLightModel
            colorModel = ColorModel.STATIC_COLOR
            isAlpha = true
            clipMethod = LocalPlaneClip(6)
        }.apply {
            blurHelper = component.root.createBlurHelper()
        }
    }

    override fun updateUi(ctx: KoolContext) {
        super.updateUi(ctx)
        val bs = shader
        if (bs is BlurShader) {
            bs.colorMix = bs.staticColor.w
            bs.staticColor.w = 1f
        }
    }
}

fun Shader.setDrawBounds(drawBounds: BoundingBox) {
    if (this is BasicShader) {
        (clipMethod as? LocalPlaneClip)?.apply {
            if (numPlanes == 6) {
                planes[0].set(Vec3f.X_AXIS, drawBounds.min.x)
                planes[1].set(Vec3f.NEG_X_AXIS, -drawBounds.max.x)

                planes[2].set(Vec3f.Y_AXIS, drawBounds.min.y)
                planes[3].set(Vec3f.NEG_Y_AXIS, -drawBounds.max.y)

                planes[4].set(Vec3f.Z_AXIS, drawBounds.min.z)
                planes[5].set(Vec3f.NEG_Z_AXIS, -drawBounds.max.z)
            }
        }
    }
}