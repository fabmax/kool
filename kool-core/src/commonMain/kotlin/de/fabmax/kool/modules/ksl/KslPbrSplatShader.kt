package de.fabmax.kool.modules.ksl

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ksl.blocks.*
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.ibl.EnvironmentMaps
import de.fabmax.kool.pipeline.shading.AlphaMode
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.logE
import de.fabmax.kool.util.logW

inline fun KslPbrSplatShader(block: KslPbrSplatShader.Config.Builder.() -> Unit): KslPbrSplatShader {
    val cfg = KslPbrSplatShader.Config.Builder().apply(block).build()
    return KslPbrSplatShader(cfg)
}

class KslPbrSplatShader(val cfg: Config) : KslShader("KslPbrSplatShader") {

    var splatMap by colorTexture(cfg.splatMapCfg)

    var displacement1 by texture2d(cfg.splatMaterials[0].displacementTex.textureName, cfg.splatMaterials[0].displacementTex.defaultTexture)
    var displacement2 by texture2d(cfg.splatMaterials[1].displacementTex.textureName, cfg.splatMaterials[1].displacementTex.defaultTexture)

    var color1 by colorTexture(cfg.splatMaterials[0].colorCfg)
    var color2 by colorTexture(cfg.splatMaterials[1].colorCfg)

    var normal1 by texture2d(cfg.splatMaterials[0].normalMapCfg.normalMapName, cfg.splatMaterials[0].normalMapCfg.defaultNormalMap)
    var normal2 by texture2d(cfg.splatMaterials[1].normalMapCfg.normalMapName, cfg.splatMaterials[1].normalMapCfg.defaultNormalMap)

    var arm1: Texture2d? by propertyTexture(cfg.splatMaterials[0].aoCfg)
    var arm2: Texture2d? by propertyTexture(cfg.splatMaterials[1].aoCfg)

    var ambientFactor: Color by uniformColor("uAmbientColor")
    var ambientMapOrientation: Mat3f by uniformMat3f("uAmbientTextureOri")
    // if ambient color is image based
    var ambientMap: TextureCube? by textureCube("tAmbientTexture")
    // if ambient color is dual image based
    val ambientMaps = List(2) { textureCube("tAmbientTexture_$it") }
    var ambientMapWeights by uniform2f("tAmbientWeights", Vec2f.X_AXIS)

    val reflectionMaps = List(2) { textureCube("tReflectionMap_$it") }
    var reflectionMapWeights: Vec2f by uniform2f("uReflectionWeights")
    var reflectionStrength: Vec4f by uniform4f("uReflectionStrength", Vec4f(cfg.reflectionStrength, 0f))
    var reflectionMap: TextureCube?
        get() = reflectionMaps[0].get()
        set(value) {
            reflectionMaps[0].set(value)
            reflectionMaps[1].set(value)
            reflectionMapWeights = Vec2f.X_AXIS
        }

    var brdfLut: Texture2d? by texture2d("tBrdfLut")

    init {
        check(cfg.numSplatMaterials in 2..5)
        pipelineConfig = cfg.pipelineCfg

        when (val ac = cfg.lightingCfg.ambientLight) {
            is KslLitShader.AmbientLight.Uniform -> ambientFactor = ac.ambientFactor
            is KslLitShader.AmbientLight.ImageBased -> {
                ambientMap = ac.ambientMap
                ambientFactor = ac.ambientFactor
            }
            is KslLitShader.AmbientLight.DualImageBased -> {
                ambientFactor = ac.ambientFactor
            }
        }
        reflectionMap = cfg.reflectionMap

        program.makeProgram()
    }

    override fun createPipeline(mesh: Mesh, instances: MeshInstanceList?, ctx: KoolContext): DrawPipeline {
        return super.createPipeline(mesh, instances, ctx).also {
            if (brdfLut == null) {
                brdfLut = ctx.defaultPbrBrdfLut
            }
        }
    }

    private fun KslProgram.makeProgram() {
        val camData = cameraData()
        val positionWorldSpace = interStageFloat3()
        val normalWorldSpace = interStageFloat3()
        val tangentWorldSpace = interStageFloat4()
        val projPosition = interStageFloat4()

        val texCoordBlock: TexCoordAttributeBlock
        val shadowMapVertexStage: ShadowBlockVertexStage?

        vertexStage {
            main {
                val vertexBlock = vertexTransformBlock(cfg.vertexCfg) {
                    inLocalPos(vertexAttribFloat3(Attribute.POSITIONS.name))
                    inLocalNormal(vertexAttribFloat3(Attribute.NORMALS.name))
                    inLocalTangent(vertexAttribFloat4(Attribute.TANGENTS.name))
                }

                // world position and normal are made available via ports for custom models to modify them
                val worldPos = float3Port("worldPos", vertexBlock.outWorldPos)
                val worldNormal = float3Port("worldNormal", vertexBlock.outWorldNormal)

                positionWorldSpace.input set worldPos
                normalWorldSpace.input set worldNormal
                tangentWorldSpace.input set vertexBlock.outWorldTangent
                projPosition.input set (camData.viewProjMat * float4Value(worldPos, 1f))
                outPosition set projPosition.input

                // texCoordBlock is used by various other blocks to access texture coordinate vertex
                // attributes (usually either none, or Attribute.TEXTURE_COORDS but there can be more)
                texCoordBlock = texCoordAttributeBlock()

                // project coordinates into shadow map / light space
                val perFragmentShadow = cfg.isParallax
                shadowMapVertexStage = if (perFragmentShadow || cfg.lightingCfg.shadowMaps.isEmpty()) null else {
                    vertexShadowBlock(cfg.lightingCfg) {
                        inPositionWorldSpace(worldPos)
                        inNormalWorldSpace(worldNormal)
                    }
                }
            }
        }

        fragmentStage {
            val lightData = sceneLightData(cfg.lightingCfg.maxNumberOfLights)
            val fnGetStochasticUv = getStochasticUv()

            main {
                val fragWorldPos = positionWorldSpace.output
                val uv = float2Var(texCoordBlock.getTextureCoords())
                val ddx = float2Var(dpdx(uv))
                val ddy = float2Var(dpdy(uv))

                val splatMapBlock = fragmentColorBlock(cfg.splatMapCfg, ddx, ddy, uv)
                val weights = decodeWeights(splatMapBlock.outColor)

                val matStates = cfg.splatMaterials.map { matCfg ->
                    val matUv = float2Var(uv * matCfg.uvScale.const)
                    val matDdx = float2Var(ddx * matCfg.uvScale.const)
                    val matDdy = float2Var(ddy * matCfg.uvScale.const)
                    val blendInfo = float4Var(Vec4f.ZERO.const)
                    SplatMatState(matCfg, matUv, matDdx, matDdy, blendInfo, fnGetStochasticUv, parentStage)
                }

                val maxHeight = float1Var(0f.const)
                val selectedMat = int1Var(0.const)
                matStates.forEach { matState ->
                    `if`(weights[matState.index] gt maxHeight) {
                        matState.sampleHeight(weights[matState.index])
                        `if`(matState.weightedHeight gt maxHeight) {
                            maxHeight set matState.weightedHeight
                            selectedMat set matState.index.const
                        }
                    }
                }

                val baseColor = float4Var()
                val normal = float3Var(normalWorldSpace.output)
                val arm = float3Var(float3Value(1f, 0.5f, 0f))
                matStates.forEach { matState ->
                    matState.sampleMaterialIfSelected(selectedMat, tangentWorldSpace.output, baseColor, normal, arm)
                }




                // create an array with light strength values per light source (1.0 = full strength)
                val shadowFactors = float1Array(lightData.maxLightCount, 1f.const)
                // adjust light strength values by shadow maps
                if (shadowMapVertexStage != null) {
                    fragmentShadowBlock(shadowMapVertexStage, shadowFactors)
                } else if (cfg.lightingCfg.shadowMaps.isNotEmpty()) {
                    fragmentOnlyShadowBlock(cfg.lightingCfg, fragWorldPos, normal, shadowFactors)
                }

                val irradiance = when (cfg.lightingCfg.ambientLight) {
                    is KslLitShader.AmbientLight.Uniform -> uniformFloat4("uAmbientColor").rgb
                    is KslLitShader.AmbientLight.ImageBased -> {
                        val ambientOri = uniformMat3("uAmbientTextureOri")
                        val ambientTex = textureCube("tAmbientTexture")
                        (sampleTexture(ambientTex, ambientOri * normal, 0f.const) * uniformFloat4("uAmbientColor")).rgb
                    }
                    is KslLitShader.AmbientLight.DualImageBased -> {
                        val ambientOri = uniformMat3("uAmbientTextureOri")
                        val ambientTexs = List(2) { textureCube("tAmbientTexture_$it") }
                        val ambientWeights = uniformFloat2("tAmbientWeights")
                        val ambientColor = float4Var(sampleTexture(ambientTexs[0], ambientOri * normal, 0f.const) * ambientWeights.x)
                        `if`(ambientWeights.y gt 0f.const) {
                            ambientColor += float4Var(sampleTexture(ambientTexs[1], ambientOri * normal, 0f.const) * ambientWeights.y)
                        }
                        (ambientColor * uniformFloat4("uAmbientColor")).rgb
                    }
                }

                val ambientOri = uniformMat3("uAmbientTextureOri")
                val brdfLut = texture2d("tBrdfLut")
                val reflectionStrength = uniformFloat4("uReflectionStrength").rgb
                val reflectionMaps = if (cfg.isTextureReflection) {
                    List(2) { textureCube("tReflectionMap_$it") }
                } else {
                    null
                }

                val material = pbrMaterialBlock(cfg.lightingCfg.maxNumberOfLights, reflectionMaps, brdfLut) {
                    inCamPos(camData.position)
                    inNormal(normal)
                    inFragmentPos(fragWorldPos)
                    inBaseColor(baseColor)

                    inRoughness(arm.y)
                    inMetallic(arm.z)

                    inIrradiance(irradiance)
                    inAoFactor(arm.x)
                    inAmbientOrientation(ambientOri)

                    inReflectionMapWeights(uniformFloat2("uReflectionWeights"))
                    inReflectionStrength(reflectionStrength)

                    setLightData(lightData, shadowFactors, cfg.lightingCfg.lightStrength.const)
                }
                val materialColor = float4Var(float4Value(material.outColor, baseColor.a))

                // set fragment stage output color
                val outRgb = float3Var(materialColor.rgb)
                if (cfg.pipelineCfg.blendMode == BlendMode.BLEND_PREMULTIPLIED_ALPHA) {
                    outRgb set outRgb * materialColor.a
                }
                outRgb set convertColorSpace(outRgb, cfg.colorSpaceConversion)

                when (cfg.alphaMode) {
                    is AlphaMode.Blend -> colorOutput(outRgb, materialColor.a)
                    is AlphaMode.Mask -> colorOutput(outRgb, 1f.const)
                    is AlphaMode.Opaque -> colorOutput(outRgb, 1f.const)
                }
            }
        }
    }

    private fun KslScopeBuilder.decodeWeights(encodedWeights: KslExprFloat4): KslArrayScalar<KslFloat1> {
        val weights = float1Array(cfg.numSplatMaterials, 0f.const)

        val r = encodedWeights.r
        val g = encodedWeights.g
        val b = encodedWeights.b
        val a = encodedWeights.a

        weights[0] set when (cfg.numSplatMaterials) {
            2 -> 1f.const - r
            3 -> 1f.const - min(1f.const, r + g)
            4 -> 1f.const - min(1f.const, r + g + b)
            5 -> 1f.const - min(1f.const, r + g + b + a)
            else -> error("invalid number of splat materials: ${cfg.numSplatMaterials}")
        }
        weights[1] set r
        if (cfg.numSplatMaterials >= 3) weights[2] set g
        if (cfg.numSplatMaterials >= 4) weights[3] set b
        if (cfg.numSplatMaterials == 5) weights[4] set a
        return weights
    }

    private fun KslShaderStage.getStochasticUv() = functionFloat4("getStochasticUv") {
        val inputUv = paramFloat2()
        val ddx = paramFloat2()
        val ddy = paramFloat2()
        val scaleRot = paramFloat2()
        val dispTex = paramColorTex2d()

        body {
            val uv = float2Var(inputUv * 3.464f.const * scaleRot.x)

            // skew input space into simplex triangle grid
            val gridToSkewedGrid = mat2Value(float2Value(1f, -0.57735026f), float2Value(0f, 1.1547005f))
            val skewedCoord = float2Var(gridToSkewedGrid * uv)

            val vertex1 = int2Var()
            val vertex2 = int2Var()
            val vertex3 = int2Var()
            val w = float3Var()

            // compute local triangle vertex IDs and local barycentric coordinates
            val baseId = int2Var(floor(skewedCoord).toInt2())
            val temp = float3Var(float3Value(fract(skewedCoord), 0f.const))
            temp.z set 1f.const - temp.x - temp.y
            `if`(temp.z gt 0f.const) {
                w.set(float3Value(temp.z, temp.y, temp.x))
                vertex1 set baseId
                vertex2 set baseId + int2Value(0, 1)
                vertex3 set baseId + int2Value(1, 0)
            }.`else` {
                w.set(float3Value(-temp.z, 1f.const - temp.y, 1f.const - temp.x))
                vertex1 set baseId + int2Value(1, 1)
                vertex2 set baseId + int2Value(1, 0)
                vertex3 set baseId + int2Value(0, 1)
            }

            // compute shifted (and rotated) uvs from hashed vertex ids
            val noise = float3Array(3, Vec3f.ZERO.const)
            noise[0] set noise32(vertex1.toFloat2())
            noise[1] set noise32(vertex2.toFloat2())
            noise[2] set noise32(vertex3.toFloat2())

            noise[0].z set (noise[0].z - 0.5f.const) * scaleRot.y
            noise[1].z set (noise[1].z - 0.5f.const) * scaleRot.y
            noise[2].z set (noise[2].z - 0.5f.const) * scaleRot.y

            val r1 = rotationMat(noise[0].z)
            val r2 = rotationMat(noise[1].z)
            val r3 = rotationMat(noise[2].z)

            val shiftedUvs = float2Array(3, Vec2f.ZERO.const)
            shiftedUvs[0] set r1 * (inputUv + noise[0].xy)
            shiftedUvs[1] set r2 * (inputUv + noise[1].xy)
            shiftedUvs[2] set r3 * (inputUv + noise[2].xy)

            // select shifted uv with the highest displacement value
            val selected = int1Var(0.const)
            val h = float1Var(sampleTextureGrad(dispTex, shiftedUvs[0], ddx, ddy).x * w.x)
            `if`(h lt w.y) {
                val ht = float1Var(sampleTextureGrad(dispTex, shiftedUvs[1], ddx, ddy).x * w.y)
                `if`(ht gt h) {
                    h set ht
                    selected set 1.const
                }
            }
            `if`(h lt w.z) {
                val ht = float1Var(sampleTextureGrad(dispTex, shiftedUvs[2], ddx, ddy).x * w.z)
                `if`(ht gt h) {
                    h set ht
                    selected set 2.const
                }
            }

            float4Value(shiftedUvs[selected], h, noise[selected].z)
        }
    }

    private fun KslScopeBuilder.rotationMat(angle: KslExprFloat1): KslExprMat2 {
        val cos = float1Var(cos(angle))
        val sin = float1Var(sin(angle))
        return mat2Var(mat2Value(float2Value(cos, sin), float2Value(-sin, cos)))
    }

    private inner class SplatMatState(
        val splatMatCfg: SplatMaterialConfig,
        val inputUv: KslExprFloat2,
        val ddx: KslExprFloat2,
        val ddy: KslExprFloat2,
        val blendInfo: KslVarVector<KslFloat4, KslFloat1>,
        val fnGetStochasticUv: KslFunctionFloat4,
        shaderStage: KslShaderStage
    ) {
        val index: Int get() = splatMatCfg.materialIndex
        val displacementTex = shaderStage.program.texture2d(splatMatCfg.displacementTex.textureName)

        val uv: KslExprFloat2 get() = blendInfo.xy
        val height: KslExprFloat1 get() = blendInfo.z
        var weightedHeight: KslExprFloat1 = KslValueFloat1(0f)

        context(KslScopeBuilder)
        fun sampleHeight(weight: KslExprFloat1) {
            blendInfo set fnGetStochasticUv(inputUv, ddx, ddy, float2Value(splatMatCfg.stochasticTileSize, splatMatCfg.stochasticTileRotation.rad), displacementTex)
            weightedHeight = float1Var(height * weight)
        }

        context(KslScopeBuilder)
        fun sampleMaterialIfSelected(
            selectedMat: KslExprInt1,
            inTangent: KslExprFloat4,
            outBaseColor: KslVarVector<KslFloat4, KslFloat1>,
            outNormal: KslVarVector<KslFloat3, KslFloat1>,
            outArm: KslVarVector<KslFloat3, KslFloat1>
        ) {
            `if`(index.const eq selectedMat) {
                fragmentColorBlock(splatMatCfg.colorCfg, ddx, ddy, uv).apply {
                    outBaseColor set outColor
                }
                if (splatMatCfg.normalMapCfg.isNormalMapped) {
                    normalMapBlock(splatMatCfg.normalMapCfg, ddx, ddy) {
                        inTexCoords(uv)
                        inNormalWorldSpace(outNormal)
                        inTangentWorldSpace(inTangent)
                    }.apply {
                        outNormal set outBumpNormal
                    }
                }
                fragmentPropertyBlock(splatMatCfg.aoCfg, ddx, ddy, uv).apply { outArm.x set outProperty }
                fragmentPropertyBlock(splatMatCfg.roughnessCfg, ddx, ddy, uv).apply { outArm.y set outProperty }
                fragmentPropertyBlock(splatMatCfg.metallicCfg, ddx, ddy, uv).apply { outArm.z set outProperty }
            }
        }
    }

    class Config(builder: Builder) {
        val pipelineCfg = builder.pipelineCfg.build()
        val vertexCfg = builder.vertexCfg.build()
        val lightingCfg = builder.lightingCfg.build()

        val splatMapCfg = builder.splatMapCfg.build()
        val splatMaterials = builder.splatMaterials.toList()
        val numParallaxSteps = builder.numParallaxSteps
        val numSplatMaterials: Int get() = splatMaterials.size

        val isTextureReflection = builder.isTextureReflection
        val reflectionStrength = builder.reflectionStrength
        val reflectionMap = builder.reflectionMap

        var colorSpaceConversion = builder.colorSpaceConversion
        var alphaMode = builder.alphaMode

        var modelCustomizer = builder.modelCustomizer

        val isParallax: Boolean get() = numParallaxSteps > 0

        open class Builder {
            val pipelineCfg = PipelineConfig.Builder()
            val vertexCfg = BasicVertexConfig.Builder()
            val lightingCfg = LightingConfig.Builder()

            val splatMapCfg = ColorBlockConfig.Builder("splatMap")
            private val _splatMaterials = mutableListOf<SplatMaterialConfig>()
            val splatMaterials: List<SplatMaterialConfig> get() = _splatMaterials
            var numParallaxSteps = 0

            var isTextureReflection = false
            var reflectionStrength = Vec3f.ONES
            var reflectionMap: TextureCube? = null
                set(value) {
                    field = value
                    isTextureReflection = value != null
                }

            var colorSpaceConversion: ColorSpaceConversion = ColorSpaceConversion.LinearToSrgbHdr()
            var alphaMode: AlphaMode = AlphaMode.Blend

            var modelCustomizer: (KslProgram.() -> Unit)? = null

            init {
                useSplatMap(null)
            }

            inline fun pipeline(block: PipelineConfig.Builder.() -> Unit) {
                pipelineCfg.block()
            }

            inline fun vertices(block: BasicVertexConfig.Builder.() -> Unit) {
                vertexCfg.block()
            }

            inline fun lighting(block: LightingConfig.Builder.() -> Unit) {
                lightingCfg.block()
            }

            inline fun splatMap(block: ColorBlockConfig.Builder.() -> Unit) {
                splatMapCfg.colorSources.clear()
                splatMapCfg.block()
            }

            fun useSplatMap(texture2d: Texture2d?) = splatMap { textureData(texture2d) }

            fun enableImageBasedLighting(iblMaps: EnvironmentMaps): Builder {
                lightingCfg.imageBasedAmbientLight(iblMaps.irradianceMap)
                reflectionMap = iblMaps.reflectionMap
                return this
            }

            fun enableParallax(numSteps: Int = 16) {
                numParallaxSteps = numSteps
            }

            fun addSplatMaterial(block: SplatMaterialConfig.Builder.() -> Unit) {
                if (splatMaterials.size >= 5) {
                    logE { "Maximum number of splat materials reached (max: 5 materials)" }
                    return
                }
                val builder = SplatMaterialConfig.Builder(splatMaterials.size).apply(block)
                _splatMaterials += builder.build()
            }

            fun build(): Config {
                if (splatMaterials.size < 2) {
                    logW { "KslPbrSplatShader requires at least 2 splat materials" }
                    while (splatMaterials.size < 2) {
                        addSplatMaterial {  }
                    }
                }
                return Config(this)
            }
        }
    }

    data class SplatMaterialConfig(
        val materialIndex: Int,
        val displacementTex: PropertyBlockConfig.TextureProperty,
        val colorCfg: ColorBlockConfig,
        val normalMapCfg: NormalMapConfig,
        val aoCfg: PropertyBlockConfig,
        val roughnessCfg: PropertyBlockConfig,
        val metallicCfg: PropertyBlockConfig,
        val emissionCfg: ColorBlockConfig,

        val uvScale: Float,
        val stochasticTileSize: Float,
        val stochasticTileRotation: AngleF,
    ) {
        class Builder(val materialIndex: Int) {
            var displacementTex = PropertyBlockConfig.TextureProperty(null, 0, "displacement_$materialIndex", PropertyBlockConfig.BlendMode.Set)
            val colorCfg = ColorBlockConfig.Builder("color_$materialIndex")
            val normalMapCfg = NormalMapConfig.Builder("normalMap_$materialIndex")
            val aoCfg = PropertyBlockConfig.Builder("ao_$materialIndex")
            val roughnessCfg = PropertyBlockConfig.Builder("rough_$materialIndex")
            val metallicCfg = PropertyBlockConfig.Builder("metal_$materialIndex")
            val emissionCfg = ColorBlockConfig.Builder("emission_$materialIndex")

            var uvScale: Float = 10f
            var stochasticTileSize: Float = 0.5f
            var stochasticTileRotation: AngleF = 360f.deg

            fun normalMap(texture2d: Texture2d?): Builder {
                normalMapping { setNormalMap(texture2d) }
                return this
            }

            fun displacement(texture2d: Texture2d?): Builder {
                displacementTex = PropertyBlockConfig.TextureProperty(texture2d, 0, "displacement_$materialIndex", PropertyBlockConfig.BlendMode.Set)
                return this
            }

            inline fun ao(block: PropertyBlockConfig.Builder.() -> Unit) {
                aoCfg.block()
            }

            inline fun color(block: ColorBlockConfig.Builder.() -> Unit) {
                colorCfg.colorSources.clear()
                colorCfg.block()
            }

            inline fun emission(block: ColorBlockConfig.Builder.() -> Unit) {
                emissionCfg.colorSources.clear()
                emissionCfg.block()
            }

            inline fun normalMapping(block: NormalMapConfig.Builder.() -> Unit) {
                normalMapCfg.block()
            }

            fun metallic(block: PropertyBlockConfig.Builder.() -> Unit) {
                metallicCfg.propertySources.clear()
                metallicCfg.block()
            }

            fun metallic(value: Float): Builder {
                metallic { constProperty(value) }
                return this
            }

            fun roughness(block: PropertyBlockConfig.Builder.() -> Unit) {
                roughnessCfg.propertySources.clear()
                roughnessCfg.block()
            }

            fun roughness(value: Float): Builder {
                roughness { constProperty(value) }
                return this
            }


            fun build(): SplatMaterialConfig {
                return SplatMaterialConfig(
                    materialIndex = materialIndex,
                    displacementTex = displacementTex,
                    colorCfg = colorCfg.build(),
                    normalMapCfg = normalMapCfg.build(),
                    aoCfg = aoCfg.build(),
                    roughnessCfg = roughnessCfg.build(),
                    metallicCfg = metallicCfg.build(),
                    emissionCfg = emissionCfg.build(),
                    uvScale = uvScale,
                    stochasticTileSize = stochasticTileSize,
                    stochasticTileRotation = stochasticTileRotation
                )
            }
        }
    }
}