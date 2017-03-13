package de.fabmax.kool.demo

import de.fabmax.kool.platform.RenderContext
import de.fabmax.kool.scene.*
import de.fabmax.kool.scene.ui.*
import de.fabmax.kool.shading.ColorModel
import de.fabmax.kool.shading.LightModel
import de.fabmax.kool.shading.basicShader
import de.fabmax.kool.util.*

/**
 * @author fabmax
 */

fun uiDemo(ctx: RenderContext) {

//    ctx.scene.camera = OrthographicCamera().apply {
//        clipToViewport = true
//    }

    ctx.clearColor = color("00323F")
    ctx.scene.camera = PerspectiveCamera().apply {
        position.set(0f, 0f, 15f)
    }

    // Create scene contents
    ctx.scene.root = group {
        +sphericalInputTransform { +ctx.scene.camera }

        +transformGroup {
            onRender += { ctx ->
                setIdentity()
                translate(0f, 0f, -7f)
                rotate((ctx.time * 60).toFloat(), Vec3f.X_AXIS)
                rotate((ctx.time * 17).toFloat(), Vec3f.Y_AXIS)
            }
            +colorMesh {
                generator = {
                    scale(5f, 5f, 5f)
                    cube {
                        centerOrigin()
                        colorCube()
                    }
                }
            }
        }

        +UiRoot(300f).apply {
            theme = UiTheme.DARK

            translate(-globalWidth /2, -globalHeight/2, 0f)
            scaleContentTo(dp(400f))

            for (i in 1..3) {
                +Button("button $i").apply {
                    layoutSpec.setOrigin(dp(50f), pc(-30f * i), un(0f))
                    layoutSpec.setSize(dp(300f), pc(20f), un(0f))

                    //font = uiFont(Font.SYSTEM_FONT, 24f, uiDpi)
                    //textColor = Color.WHITE
                    text = "Button " + i

                    onHoverEnter += { ptr, rt, ctx ->
                        println("$text hover enter ${rt.hitPositionLocal}")
                    }
                    onHoverExit += { ptr, rt, ctx ->
                        println("$text hover exit")
                    }
                    onHover += { ptr, rt, ctx ->
                        if (ptr.isLeftButtonEvent && !ptr.isLeftButtonDown) {
                            println("$text clicked")
                            if (i == 1) {
                                theme = UiTheme.DARK
                            } else if (i == 2) {
                                theme = UiTheme.LIGHT
                            }
                        }
                    }
                }
            }
        }
    }
    ctx.run()
}
