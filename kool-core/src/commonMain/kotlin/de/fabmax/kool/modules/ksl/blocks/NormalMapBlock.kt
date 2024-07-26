package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Texture2d

fun KslScopeBuilder.normalMapBlock(
    cfg: NormalMapConfig,
    ddx: KslExprFloat2? = null,
    ddy: KslExprFloat2? = null,
    block: NormalMapBlock.() -> Unit
): NormalMapBlock {
    val normalMapBlock = NormalMapBlock(cfg, parentStage.program.nextName("normalMapBlock"), ddx, ddy, this)
    ops += normalMapBlock.apply(block)
    return normalMapBlock
}

class NormalMapBlock(
    cfg: NormalMapConfig,
    name: String,
    inDdx: KslExprFloat2?,
    inDdy: KslExprFloat2?,
    parentScope: KslScopeBuilder
) : KslBlock(name, parentScope) {

    val inNormalWorldSpace = inFloat3("inNormalWorldSpace")
    val inTangentWorldSpace = inFloat4("inTangentWorldSpace")
    val inTexCoords = inFloat2("inTexCoords")
    val inStrength = inFloat1("inStrength", KslValueFloat1(1f))

    val outBumpNormal = outFloat3()

    init {
        body.apply {
            if (cfg.isNormalMapped) {
                val normalMap = parentStage.program.texture2d(cfg.normalMapName)
                val sample = if (inDdx != null && inDdy != null) {
                    sampleTextureGrad(normalMap, inTexCoords, inDdx, inDdy)
                } else {
                    sampleTexture(normalMap, inTexCoords)
                }
                val mapNormal = float3Var(normalize(sample.xyz * 2f.const - 1f.const))
                outBumpNormal set calcBumpedNormal(inNormalWorldSpace, inTangentWorldSpace, mapNormal, inStrength)

            } else {
                outBumpNormal set inNormalWorldSpace
            }
        }
    }
}

data class NormalMapConfig(
    val isNormalMapped: Boolean,
    val normalMapName: String,
    val defaultNormalMap: Texture2d?,
    val strengthCfg: PropertyBlockConfig
) {
    class Builder(var normalMapName: String = "tNormalMap") {
        var isNormalMapped: Boolean = false
        var defaultNormalMap: Texture2d? = null
        val strengthCfg: PropertyBlockConfig.Builder = PropertyBlockConfig.Builder("${normalMapName}_strength").constProperty(1f)

        fun clearNormalMap(): Builder {
            isNormalMapped = false
            defaultNormalMap = null
            return this
        }

        fun setNormalMap(texture: Texture2d? = null): Builder {
            this.isNormalMapped = true
            this.defaultNormalMap = texture
            return this
        }

        fun build() = NormalMapConfig(isNormalMapped, normalMapName, defaultNormalMap, strengthCfg.build())
    }
}
