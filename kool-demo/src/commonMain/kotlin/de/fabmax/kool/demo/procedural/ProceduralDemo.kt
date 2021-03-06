package de.fabmax.kool.demo.procedural

import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.Demo
import de.fabmax.kool.demo.DemoScene
import de.fabmax.kool.demo.controlUi
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.randomI
import de.fabmax.kool.scene.Skybox
import de.fabmax.kool.scene.orbitInputTransform
import de.fabmax.kool.scene.scene
import de.fabmax.kool.scene.ui.Label
import de.fabmax.kool.util.BoundingBox
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.SimpleShadowMap
import de.fabmax.kool.util.deferred.DeferredPipeline
import de.fabmax.kool.util.deferred.DeferredPipelineConfig
import de.fabmax.kool.util.ibl.EnvironmentHelper

class ProceduralDemo : DemoScene("Procedural Geometry") {
    var autoRotate = true
    lateinit var roses: Roses

    override fun setupMainScene(ctx: KoolContext) = scene {
        +orbitInputTransform {
            setMouseRotation(-20f, -10f)
            setMouseTranslation(0f, 16f, 0f)
            zoom = 40.0
            +camera

            onUpdate += {
                if (autoRotate) {
                    verticalRotation += 5f * it.deltaT
                }
            }
        }

        lighting.singleLight {
            setDirectional(Vec3f(-1f, -0.3f, -1f))
            setColor(Color.MD_AMBER.mix(Color.WHITE, 0.5f).toLinear(), 3f)
        }

        ctx.assetMgr.launch {
            val ibl = EnvironmentHelper.hdriEnvironment(this@scene, "${Demo.envMapBasePath}/syferfontein_0d_clear_1k.rgbe.png", this)
            +Skybox.cube(ibl.reflectionMap, 1f)

            val shadowMap = SimpleShadowMap(this@scene, 0).apply {
                setDefaultDepthOffset(true)
                shadowBounds = BoundingBox(Vec3f(-30f, 0f, -30f), Vec3f(30f, 60f, 30f))
            }
            val deferredCfg = DeferredPipelineConfig().apply {
                isWithScreenSpaceReflections = true
                isWithAmbientOcclusion = true
                isWithEmissive = true
                maxGlobalLights = 1
                useImageBasedLighting(ibl)
                useShadowMaps(listOf(shadowMap))
            }
            val deferredPipeline = DeferredPipeline(this@scene, deferredCfg).apply {
                aoPipeline?.radius = 0.6f

                contentGroup.apply {
                    +Glas(pbrPass.colorTexture!!, ibl)
                    +Vase()
                    +Table()

                    roses = Roses()
                    +roses
                }
            }
            shadowMap.drawNode = deferredPipeline.contentGroup
            +deferredPipeline.renderOutput
        }
    }

    override fun setupMenu(ctx: KoolContext) = controlUi(ctx) {
        section("Roses") {
            button("Empty Vase") {
                roses.children.forEach { it.dispose(ctx) }
                roses.removeAllChildren()
            }
            var seedTxt: Label? = null
            var replaceLastRose = true
            button("Generate Rose") {
                if (roses.children.isNotEmpty() && replaceLastRose) {
                    val remNd = roses.children.last()
                    roses.removeNode(remNd)
                    remNd.dispose(ctx)
                }

                val seed = randomI()
                seedTxt?.text = "$seed"
                roses.makeRose(seed)
            }
            toggleButton("Replace Last Rose", replaceLastRose) { replaceLastRose = isEnabled }
            seedTxt = textWithValue("Seed:", "")
        }
        section("Scene") {
            toggleButton("Auto Rotate", autoRotate) { autoRotate = isEnabled }
        }
    }
}