package de.fabmax.kool.demo

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Random
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.GlslType
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.pipeline.shadermodel.*
import de.fabmax.kool.pipeline.shading.ModeledShader
import de.fabmax.kool.pipeline.shading.PbrMaterialConfig
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.*
import de.fabmax.kool.util.deferred.*
import kotlin.math.sqrt

class DeferredDemo() : DemoScene("Deferred Shading") {

    private lateinit var deferredPipeline: DeferredPipeline

    private lateinit var objects: Mesh
    private lateinit var objectShader: DeferredPbrShader

    private lateinit var lightPositionMesh: Mesh
    private lateinit var lightVolumeMesh: LineMesh

    private var autoRotate = true
    private val rand = Random(1337)

    private var lightCount = 2000
    private val lights = mutableListOf<AnimatedLight>()

    private val colorMap = Cycler(listOf(
            ColorMap("Colorful", listOf(Color.MD_RED, Color.MD_PINK, Color.MD_PURPLE, Color.MD_DEEP_PURPLE,
                    Color.MD_INDIGO, Color.MD_BLUE, Color.MD_LIGHT_BLUE, Color.MD_CYAN, Color.MD_TEAL, Color.MD_GREEN,
                    Color.MD_LIGHT_GREEN, Color.MD_LIME, Color.MD_YELLOW, Color.MD_AMBER, Color.MD_ORANGE, Color.MD_DEEP_ORANGE)),
            ColorMap("Hot-Cold", listOf(Color.MD_PINK, Color.MD_CYAN)),
            ColorMap("Summer", listOf(Color.MD_ORANGE, Color.MD_BLUE, Color.MD_GREEN)),
            ColorMap("White", listOf(Color.WHITE))
    )).apply { index = 1 }

    override fun lateInit(ctx: KoolContext) {
        updateLights()
    }

    override fun setupMainScene(ctx: KoolContext) = scene {
        +orbitInputTransform {
            // Set some initial rotation so that we look down on the scene
            setMouseRotation(0f, -40f)
            // Add camera to the transform group
            +camera
            zoom = 28.0
            maxZoom = 50.0

            translation.set(0.0, -11.0, 0.0)
            onUpdate += {
                if (autoRotate) {
                    verticalRotation += it.deltaT * 3f
                }
            }
        }

        // don't use any global lights
        lighting.lights.clear()

        val defCfg = DeferredPipelineConfig().apply {
            maxGlobalLights = 0
            isWithEmissive = false
            isWithAmbientOcclusion = true
            isWithScreenSpaceReflections = false
            isWithImageBasedLighting = false
        }
        deferredPipeline = DeferredPipeline(this, defCfg)
        deferredPipeline.contentGroup.makeContent()

        +deferredPipeline.renderOutput
        makeLightOverlays()

        onUpdate += { evt ->
            lights.forEach { it.animate(evt.deltaT) }
        }
    }

    private fun Scene.makeLightOverlays() {
        apply {
            lightPositionMesh = colorMesh {
                isFrustumChecked = false
                isVisible = true
                generate {
                    color = Color.RED
                    icoSphere {
                        steps = 1
                        radius = 0.05f
                        center.set(Vec3f.ZERO)
                    }
                }
                shader = ModeledShader(instancedLightIndicatorModel())
            }
            +lightPositionMesh

            lightVolumeMesh = wireframeMesh(deferredPipeline.pbrPass.dynamicPointLights.mesh.geometry).apply {
                isFrustumChecked = false
                isVisible = false
                shader = ModeledShader(instancedLightIndicatorModel())
            }
            +lightVolumeMesh

            val lightPosInsts = MeshInstanceList(listOf(MeshInstanceList.MODEL_MAT, Attribute.COLORS), MAX_LIGHTS)
            val lightVolInsts = MeshInstanceList(listOf(MeshInstanceList.MODEL_MAT, Attribute.COLORS), MAX_LIGHTS)
            lightPositionMesh.instances = lightPosInsts
            lightVolumeMesh.instances = lightVolInsts

            val lightModelMat = Mat4f()
            onUpdate += {
                if (lightPositionMesh.isVisible || lightVolumeMesh.isVisible) {
                    lightPosInsts.clear()
                    lightVolInsts.clear()
                    val srgbColor = MutableColor()
                    deferredPipeline.pbrPass.dynamicPointLights.lightInstances.forEach { light ->
                        lightModelMat.setIdentity()
                        lightModelMat.translate(light.position)

                        light.color.toSrgb(srgbColor)

                        if (lightPositionMesh.isVisible) {
                            lightPosInsts.addInstance {
                                put(lightModelMat.matrix)
                                put(srgbColor.array)
                            }
                        }
                        if (lightVolumeMesh.isVisible) {
                            val s = sqrt(light.intensity)
                            lightModelMat.scale(s, s, s)
                            lightVolInsts.addInstance {
                                put(lightModelMat.matrix)
                                put(srgbColor.array)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Group.makeContent() {
        objects = colorMesh {
            generate {
                val sphereProtos = mutableListOf<IndexedVertexList>()
                for (i in 0..10) {
                    val builder = MeshBuilder(IndexedVertexList(Attribute.POSITIONS, Attribute.NORMALS, Attribute.COLORS))
                    sphereProtos += builder.geometry
                    builder.apply {
                        icoSphere {
                            steps = 3
                            radius = rand.randomF(0.3f, 0.4f)
                            center.set(0f, 0.1f + radius, 0f)
                        }
                    }
                }

                for (x in -19..19) {
                    for (y in -19..19) {
                        color = Color.WHITE
                        withTransform {
                            translate(x.toFloat(), 0f, y.toFloat())
                            if ((x + 100) % 2 == (y + 100) % 2) {
                                cube {
                                    size.set(rand.randomF(0.6f, 0.8f), rand.randomF(0.6f, 0.95f), rand.randomF(0.6f, 0.8f))
                                    origin.set(-size.x / 2, 0.1f, -size.z / 2)
                                }
                            } else {
                                geometry(sphereProtos[rand.randomI(sphereProtos.indices)])
                            }
                        }
                    }
                }
            }
            val pbrCfg = PbrMaterialConfig().apply {
                roughness = 0.15f
            }
            objectShader = DeferredPbrShader(pbrCfg)
            shader = objectShader
        }
        +objects

        +textureMesh(isNormalMapped = true) {
            generate {
                rotate(90f, Vec3f.NEG_X_AXIS)
                color = Color.WHITE
                rect {
                    size.set(40f, 40f)
                    origin.set(size.x, size.y, 0f).scale(-0.5f)
                    generateTexCoords(30f)
                }
            }
            val pbrCfg = PbrMaterialConfig().apply {
                useAlbedoMap("${Demo.pbrBasePath}/futuristic-panels1/futuristic-panels1-albedo1.jpg")
                useNormalMap("${Demo.pbrBasePath}/futuristic-panels1/futuristic-panels1-normal.jpg")
                useRoughnessMap("${Demo.pbrBasePath}/futuristic-panels1/futuristic-panels1-roughness.jpg")
                useMetallicMap("${Demo.pbrBasePath}/futuristic-panels1/futuristic-panels1-metallic.jpg")
                useOcclusionMap("${Demo.pbrBasePath}/futuristic-panels1/futuristic-panels1-ao.jpg")
            }
            val groundShader = DeferredPbrShader(pbrCfg)
            shader = groundShader

            onDispose += {
                groundShader.albedoMap?.dispose()
                groundShader.occlusionMap?.dispose()
                groundShader.normalMap?.dispose()
                groundShader.metallicMap?.dispose()
                groundShader.roughnessMap?.dispose()
                groundShader.displacementMap?.dispose()
            }
        }
    }

    private fun updateLights(forced: Boolean = false) {
        val rows = 41
        val travel = rows.toFloat()
        val start = travel / 2

        val objOffset = if (objects.isVisible) 0.7f else 0f
        val lightGroups = listOf(
                LightGroup(Vec3f(1 - start, 0.45f, -start), Vec3f(1f, 0f, 0f), Vec3f(0f, 0f, 1f), rows - 1),
                LightGroup(Vec3f(-start, 0.45f, 1 - start), Vec3f(0f, 0f, 1f), Vec3f(1f, 0f, 0f), rows - 1),

                LightGroup(Vec3f(1.5f - start, 0.45f + objOffset, start), Vec3f(1f, 0f, 0f), Vec3f(0f, 0f, -1f), rows - 2),
                LightGroup(Vec3f(start, 0.45f + objOffset, 1.5f - start), Vec3f(0f, 0f, 1f), Vec3f(-1f, 0f, 0f), rows - 2)
        )

        if (forced) {
            lights.clear()
            deferredPipeline.pbrPass.dynamicPointLights.lightInstances.clear()
        } else {
            while (lights.size > lightCount) {
                lights.removeAt(lights.lastIndex)
                deferredPipeline.pbrPass.dynamicPointLights.lightInstances.removeAt(deferredPipeline.pbrPass.dynamicPointLights.lightInstances.lastIndex)
            }
        }

        while (lights.size < lightCount) {
            val grp = lightGroups[rand.randomI(lightGroups.indices)]
            val x = rand.randomI(0 until grp.rows)
            val light = deferredPipeline.pbrPass.dynamicPointLights.addPointLight {
                intensity = 1.0f
            }
            val animLight = AnimatedLight(light).apply {
                startColor = colorMap.current.getColor(lights.size).toLinear()
                desiredColor = startColor
                colorMix = 1f
            }
            lights += animLight
            grp.setupLight(animLight, x, travel, rand.randomF())
        }
        updateLightColors()
    }

    private fun updateLightColors() {
        lights.forEachIndexed { iLight, it ->
            it.startColor = it.desiredColor
            it.desiredColor = colorMap.current.getColor(iLight).toLinear()
            it.colorMix = 0f
        }
    }

    override fun setupMenu(ctx: KoolContext) = controlUi(ctx) {
        val images = mutableListOf<UiImage>()
        images += image(imageShader = gBufferShader(deferredPipeline.mrtPass.albedoMetal, 0f, 1f)).apply {
            setupImage(0.025f, 0.025f)
        }
        images += image(imageShader = gBufferShader(deferredPipeline.mrtPass.normalRoughness, 1f, 0.5f)).apply {
            setupImage(0.025f, 0.35f)
        }
        images += image(imageShader = gBufferShader(deferredPipeline.mrtPass.positionAo, 10f, 0.05f)).apply {
            setupImage(0.025f, 0.675f)
        }
        images += image(imageShader = ModeledShader.TextureColor(deferredPipeline.aoPipeline?.aoMap, model = AoDemo.aoMapColorModel())).apply {
            setupImage(0.35f, 0.35f)
        }
        images += image(imageShader = MetalRoughAoTex(deferredPipeline.mrtPass)).apply {
            setupImage(0.35f, 0.675f)
        }

        section("Dynamic Lights") {
            sliderWithValue("Light Count:", lightCount.toFloat(), 1f, MAX_LIGHTS.toFloat(), 0) {
                lightCount = value.toInt()
                updateLights()
            }
            toggleButton("Light Positions", lightPositionMesh.isVisible) { lightPositionMesh.isVisible = isEnabled }
            toggleButton("Light Volumes", lightVolumeMesh.isVisible) { lightVolumeMesh.isVisible = isEnabled }
            cycler("Color Map:", colorMap) { _, _ -> updateLightColors() }
        }
        section("Deferred Shading") {
            toggleButton("Show Maps", images.first().isVisible) { images.forEach { it.isVisible = isEnabled } }
            toggleButton("Ambient Occlusion", deferredPipeline.isAoEnabled) { deferredPipeline.isAoEnabled = isEnabled }
        }
        section("Scene") {
            toggleButton("Auto Rotate", autoRotate) { autoRotate = isEnabled}
            toggleButton("Show Objects", objects.isVisible) {
                objects.isVisible = isEnabled
                updateLights(true)
            }
            sliderWithValue("Object Roughness:", objectShader.roughness, 0f, 1f, 2) {
                objectShader.roughness = value
            }
        }
    }

    private fun UiImage.setupImage(x: Float, y: Float) {
        isVisible = false
        relativeWidth = 0.3f
        relativeX = x
        relativeY = y
    }

    private inner class LightGroup(val startConst: Vec3f, val startIt: Vec3f, val travelDir: Vec3f, val rows: Int) {
        fun setupLight(light: AnimatedLight, x: Int, travelDist: Float, travelPos: Float) {
            light.startPos.set(startIt).scale(x.toFloat()).add(startConst)
            light.dir.set(travelDir)

            light.travelDist = travelDist
            light.travelPos = travelPos * travelDist
            light.speed = rand.randomF(1f, 3f) * 0.25f
        }
    }

    private class AnimatedLight(val light: DeferredPointLights.PointLight) {
        val startPos = MutableVec3f()
        val dir = MutableVec3f()
        var speed = 1.5f
        var travelPos = 0f
        var travelDist = 10f

        var startColor = Color.WHITE
        var desiredColor = Color.WHITE
        var colorMix = 0f

        fun animate(deltaT: Float) {
            travelPos += deltaT * speed
            if (travelPos > travelDist) {
                travelPos -= travelDist
            }
            light.position.set(dir).scale(travelPos).add(startPos)

            if (colorMix < 1f) {
                colorMix += deltaT * 2f
                if (colorMix > 1f) {
                    colorMix = 1f
                }
                startColor.mix(desiredColor, colorMix, light.color)
            }
        }
    }

    private class ColorMap(val name: String, val colors: List<Color>) {
        fun getColor(idx: Int): Color = colors[idx % colors.size]
        override fun toString() = name
    }

    private fun instancedLightIndicatorModel(): ShaderModel = ShaderModel("instancedLightIndicators").apply {
        val ifColors: StageInterfaceNode
        vertexStage {
            ifColors = stageInterfaceNode("ifColors", instanceAttributeNode(Attribute.COLORS).output)
            val modelMvp = premultipliedMvpNode().outMvpMat
            val instMvp = multiplyNode(modelMvp, instanceAttrModelMat().output).output
            positionOutput = vec4TransformNode(attrPositions().output, instMvp).outVec4
        }
        fragmentStage {
            colorOutput(unlitMaterialNode(ifColors.output).outColor)
        }
    }

    private fun gBufferShader(map: Texture2d, offset: Float, scale: Float): ModeledShader {
        return ModeledShader.TextureColor(map, model = rgbMapColorModel(offset, scale))
    }

    private fun rgbMapColorModel(offset: Float, scale: Float) = ShaderModel("rgbMap").apply {
        val ifTexCoords: StageInterfaceNode

        vertexStage {
            ifTexCoords = stageInterfaceNode("ifTexCoords", attrTexCoords().output)
            positionOutput = simpleVertexPositionNode().outVec4
        }
        fragmentStage {
            val sampler = texture2dSamplerNode(texture2dNode("colorTex"), ifTexCoords.output)
            val rgb = splitNode(sampler.outColor, "rgb").output
            val scaled = multiplyNode(addNode(rgb, ShaderNodeIoVar(ModelVar1fConst(offset))).output, scale)
            colorOutput(scaled.output, alpha = ShaderNodeIoVar(ModelVar1fConst(1f)))
        }
    }

    private class MetalRoughAoTex(val mrtPass: DeferredMrtPass) : ModeledShader(shaderModel()) {
        override fun onPipelineCreated(pipeline: Pipeline, mesh: Mesh, ctx: KoolContext) {
            model.findNode<Texture2dNode>("positionAo")!!.sampler.texture = mrtPass.positionAo
            model.findNode<Texture2dNode>("normalRough")!!.sampler.texture = mrtPass.normalRoughness
            model.findNode<Texture2dNode>("albedoMetal")!!.sampler.texture = mrtPass.albedoMetal
            super.onPipelineCreated(pipeline, mesh, ctx)
        }

        companion object {
            fun shaderModel() = ShaderModel().apply {
                val ifTexCoords: StageInterfaceNode

                vertexStage {
                    ifTexCoords = stageInterfaceNode("ifTexCoords", attrTexCoords().output)
                    positionOutput = simpleVertexPositionNode().outVec4
                }
                fragmentStage {
                    val aoSampler = texture2dSamplerNode(texture2dNode("positionAo"), ifTexCoords.output)
                    val roughSampler = texture2dSamplerNode(texture2dNode("normalRough"), ifTexCoords.output)
                    val metalSampler = texture2dSamplerNode(texture2dNode("albedoMetal"), ifTexCoords.output)
                    val ao = splitNode(aoSampler.outColor, "a").output
                    val rough = splitNode(roughSampler.outColor, "a").output
                    val metal = splitNode(metalSampler.outColor, "a").output
                    val outColor = combineNode(GlslType.VEC_3F).apply {
                        inX = ao
                        inY = rough
                        inZ = metal
                    }
                    colorOutput(outColor.output, alpha = ShaderNodeIoVar(ModelVar1fConst(1f)))
                }
            }
        }
    }

    companion object {
        const val MAX_LIGHTS = 5000
    }
}