package de.fabmax.kool.pipeline.ibl

import de.fabmax.kool.AssetLoader
import de.fabmax.kool.Assets
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.util.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class EnvironmentMap(val irradianceMap: TextureCube, val reflectionMap: TextureCube) : BaseReleasable() {
    override fun release() {
        irradianceMap.release()
        if (irradianceMap !== reflectionMap) {
            reflectionMap.release()
        }
        super.release()
    }

    companion object {
        fun fromSingleColor(color: Color): EnvironmentMap {
            val bgColor = TextureData2d.singleColor(color.toLinear())
            val props = TextureProps(
                generateMipMaps = false,
                defaultSamplerSettings = SamplerSettings().nearest()
            )
            val cubeTex = TextureCube(props, "singleColorEnv-$color", BufferedTextureLoader(TextureDataCube(bgColor, bgColor, bgColor, bgColor, bgColor, bgColor)))
            return EnvironmentMap(cubeTex, cubeTex)
        }

        fun fromGradientColor(gradient: ColorGradient): EnvironmentMap {
            val scene = KoolSystem.requireContext().backgroundScene
            val gradientTex = GradientTexture(gradient)
            val gradientPass = GradientCubeGenerator(scene, gradientTex)
            gradientTex.releaseWith(gradientPass)
            return renderPassEnvironment(gradientPass)
        }

        fun fromHdriTexture(
            hdri: Texture2d,
            brightness: Float = 1f,
            releaseHdriTexAfterConversion: Boolean = true
        ): EnvironmentMap {
            val scene = KoolSystem.requireContext().backgroundScene
            val rgbeDecoder = RgbeDecoder(scene, hdri, brightness)
            if (releaseHdriTexAfterConversion) {
                hdri.releaseWith(rgbeDecoder)
            }
            return renderPassEnvironment(rgbeDecoder)
        }

        private fun renderPassEnvironment(renderPass: OffscreenRenderPass): EnvironmentMap {
            val tex = when (renderPass) {
                is OffscreenRenderPassCube -> renderPass.colorTexture!!
                is OffscreenRenderPass2d -> renderPass.colorTexture!!
                else -> throw IllegalArgumentException("Supplied OffscreenRenderPass must be OffscreenRenderPassCube or OffscreenRenderPass2d")
            }
            val scene = KoolSystem.requireContext().backgroundScene
            val irrMapPass = IrradianceMapPass.irradianceMap(scene, tex)
            val reflMapPass = ReflectionMapPass.reflectionMap(scene, tex)

            irrMapPass.dependsOn(renderPass)
            reflMapPass.dependsOn(renderPass)

            val maps = EnvironmentMap(irrMapPass.copyColor(), reflMapPass.copyColor())
            scene.addOffscreenPass(renderPass)
            scene.addOffscreenPass(irrMapPass)
            scene.addOffscreenPass(reflMapPass)
            return maps
        }
    }
}

fun AssetLoader.hdriEnvironmentAsync(hdriPath: String, brightness: Float = 1f): Deferred<Result<EnvironmentMap>> {
    return Assets.async {
        val samplerSettings = SamplerSettings().nearest()
        val hdriTexProps = TextureProps(generateMipMaps = false, defaultSamplerSettings = samplerSettings)
        val hdri = loadTexture2d(hdriPath, hdriTexProps)
        withContext(Dispatchers.RenderLoop) {
            hdri.map { EnvironmentMap.fromHdriTexture(it, brightness) }
        }
    }
}

suspend fun AssetLoader.hdriEnvironment(hdriPath: String, brightness: Float = 1f): Result<EnvironmentMap> {
    return hdriEnvironmentAsync(hdriPath, brightness).await()
}