package de.fabmax.kool.editor.components

import de.fabmax.kool.editor.api.AppAssets
import de.fabmax.kool.editor.api.AssetReference
import de.fabmax.kool.editor.api.SceneShaderData
import de.fabmax.kool.editor.api.loadTexture2d
import de.fabmax.kool.editor.data.*
import de.fabmax.kool.modules.ksl.KslLitShader
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.ModelMatrixComposition
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.util.Color

suspend fun PbrShaderData.createPbrShader(sceneShaderData: SceneShaderData, modelMats: List<ModelMatrixComposition>): KslPbrShader {
    val shader = KslPbrShader {
        pipeline {
            if (genericSettings.isTwoSided) {
                cullMethod = CullMethod.NO_CULLING
            }
        }
        vertices {
            isInstanced = true
            modelMatrixComposition = modelMats
        }
        color {
            when (val color = baseColor) {
                is ConstColorAttribute -> uniformColor()
                is ConstValueAttribute -> uniformColor()
                is MapAttribute -> textureColor()
                is VertexAttribute -> vertexColor(Attribute(color.attribName, GpuType.FLOAT4))
            }
        }
        emission {
            when (val color = emission) {
                is ConstColorAttribute -> uniformColor()
                is ConstValueAttribute -> uniformColor()
                is MapAttribute -> textureColor()
                is VertexAttribute -> vertexColor(Attribute(color.attribName, GpuType.FLOAT4))
            }
        }
        roughness {
            when (val rough = roughness) {
                is ConstColorAttribute -> uniformProperty()
                is ConstValueAttribute -> uniformProperty()
                is MapAttribute -> textureProperty()
                is VertexAttribute -> vertexProperty(Attribute(rough.attribName, GpuType.FLOAT1))
            }
        }
        metallic {
            when (val metal = metallic) {
                is ConstColorAttribute -> uniformProperty()
                is ConstValueAttribute -> uniformProperty()
                is MapAttribute -> textureProperty()
                is VertexAttribute -> vertexProperty(Attribute(metal.attribName, GpuType.FLOAT1))
            }
        }
        this@createPbrShader.aoMap?.let {
            ao { textureProperty(null, it.singleChannelIndex) }
        }
        this@createPbrShader.displacementMap?.let {
            vertices {
                displacement {
                    uniformProperty(parallaxOffset)
                }
            }
            parallaxMapping {
                useParallaxMap(null, parallaxStrength, maxSteps = parallaxSteps, textureChannel = it.singleChannelIndex)
            }
        }
        this@createPbrShader.normalMap?.let {
            normalMapping {
                setNormalMap()
            }
        }

        lighting {
            maxNumberOfLights = sceneShaderData.maxNumberOfLights
            addShadowMaps(sceneShaderData.shadowMaps)
        }
        colorSpaceConversion = ColorSpaceConversion.LinearToSrgbHdr(sceneShaderData.toneMapping)

        sceneShaderData.environmentMaps?.let {
            enableImageBasedLighting(it)
        }
        sceneShaderData.ssaoMap?.let {
            ao { enableSsao(it) }
        }
    }
    updatePbrShader(shader, sceneShaderData)
    return shader
}

suspend fun PbrShaderData.updatePbrShader(shader: KslPbrShader, sceneShaderData: SceneShaderData): Boolean {
    if (!matchesPbrShaderConfig(shader)) {
        return false
    }
    val pbrShader = shader as? KslPbrShader ?: return false

    val colorConv = shader.cfg.colorSpaceConversion
    if (colorConv is ColorSpaceConversion.LinearToSrgbHdr && colorConv.toneMapping != sceneShaderData.toneMapping) {
        return false
    }

    val ibl = sceneShaderData.environmentMaps
    val isIbl = ibl != null
    val isSsao = sceneShaderData.ssaoMap != null
    val isMaterialAo = aoMap != null

    when {
        (shader.ambientCfg is KslLitShader.AmbientLight.ImageBased) != isIbl -> return false
        shader.isSsao != isSsao -> return false
        shader.cfg.lightingConfig.maxNumberOfLights != sceneShaderData.maxNumberOfLights -> return false
        shader.shadowMaps != sceneShaderData.shadowMaps -> return false
        (shader.materialAoCfg.primaryTexture != null) != isMaterialAo -> return false
    }

    val colorMap = (baseColor as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val roughnessMap = (roughness as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val metallicMap = (metallic as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val emissionMap = (emission as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val normalMap = normalMap?.let { AppAssets.loadTexture2d(it.mapPath) }
    val aoMap = aoMap?.let { AppAssets.loadTexture2d(it.mapPath) }
    val displacementMap = displacementMap?.let { AppAssets.loadTexture2d(AssetReference.Texture(it.mapPath, TexFormat.R)) }

    when (val color = baseColor) {
        is ConstColorAttribute -> pbrShader.color = color.color.toColorLinear()
        is ConstValueAttribute -> pbrShader.color = Color(color.value, color.value, color.value)
        is MapAttribute -> pbrShader.colorMap = colorMap
        is VertexAttribute -> { }
    }
    when (val color = emission) {
        is ConstColorAttribute -> pbrShader.emission = color.color.toColorLinear()
        is ConstValueAttribute -> pbrShader.emission = Color(color.value, color.value, color.value)
        is MapAttribute -> pbrShader.emissionMap = emissionMap
        is VertexAttribute -> { }
    }
    when (val rough = roughness) {
        is ConstColorAttribute -> pbrShader.roughness = rough.color.r
        is ConstValueAttribute -> pbrShader.roughness = rough.value
        is MapAttribute -> pbrShader.roughnessMap = roughnessMap
        is VertexAttribute -> { }
    }
    when (val metal = metallic) {
        is ConstColorAttribute -> pbrShader.metallic = metal.color.r
        is ConstValueAttribute -> pbrShader.metallic = metal.value
        is MapAttribute -> pbrShader.metallicMap = metallicMap
        is VertexAttribute -> { }
    }
    pbrShader.normalMap = normalMap
    pbrShader.materialAoMap = aoMap
    pbrShader.parallaxMap = displacementMap
    pbrShader.parallaxMapSteps = parallaxSteps
    pbrShader.parallaxStrength = parallaxStrength
    pbrShader.vertexDisplacementStrength = parallaxOffset

    if (ibl != null) {
        pbrShader.ambientFactor = Color.WHITE
        pbrShader.ambientMap = ibl.irradianceMap
        pbrShader.reflectionMap = ibl.reflectionMap
    } else {
        pbrShader.ambientFactor = sceneShaderData.ambientColorLinear
    }
    return true
}

fun PbrShaderData.matchesPbrShaderConfig(shader: DrawShader?): Boolean {
    if (shader !is KslPbrShader) {
        return false
    }
    return baseColor.matchesCfg(shader.colorCfg)
            && roughness.matchesCfg(shader.roughnessCfg)
            && metallic.matchesCfg(shader.metallicCfg)
            && emission.matchesCfg(shader.emissionCfg)
            && aoMap?.matchesCfg(shader.materialAoCfg) != false
            && shader.isParallaxMapped == (displacementMap != null)
            && shader.isNormalMapped == (normalMap != null)
            && genericSettings.matchesPipelineConfig(shader.pipelineConfig)
}
