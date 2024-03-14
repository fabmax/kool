package de.fabmax.kool.util

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolContext
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.pipeline.AsyncTextureLoader
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.pipeline.TextureProps
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-channel Signed Distance Field based font. Provides good-looking text for pretty much arbitrary font sizes from a
 * single relatively small texture. However, unlike traditional texture-atlas based fonts, the MSDF font texture has
 * to be pre-generated. See this awesome GitHub repo for more details:
 *
 *     https://github.com/Chlumsky/msdf-atlas-gen
 */
class MsdfFont(
    val data: MsdfFontData = DEFAULT_FONT_DATA,
    sizePts: Float = 12f,
    val italic: Float = ITALIC_NONE,
    val weight: Float = WEIGHT_REGULAR,
    val cutoff: Float = CUTOFF_SOLID,
    val glowColor: Color? = null
) : Font(sizePts) {

    override var scale: Float = 1f
    override val lineHeight: Float get() = scale * sizePts * data.meta.metrics.lineHeight

    private val emScale: Float
        get() = scale * sizePts

    override fun setScale(scale: Float, ctx: KoolContext) {
        this.scale = scale
    }

    override fun textDimensions(text: String, result: TextMetrics, enforceSameWidthDigits: Boolean): TextMetrics {
        var lineWidth = 0f
        result.baselineWidth = 0f
        result.height = lineHeight
        result.yBaseline = data.meta.metrics.ascender * emScale
        result.numLines = 1
        result.ascentPx = data.meta.metrics.ascender * emScale
        result.descentPx = data.meta.metrics.descender * emScale

        for (i in text.indices) {
            val c = text[i]
            if (c == '\n') {
                result.baselineWidth = max(result.width, lineWidth)
                result.height += lineHeight
                result.numLines++
                lineWidth = 0f

            } else {
                val metrics = if (c.isDigit() && enforceSameWidthDigits) {
                    data.maxWidthDigit
                } else {
                    data.glyphMap[c]
                }?: continue
                lineWidth += metrics.advance * emScale
            }
        }
        result.baselineWidth = max(result.width, lineWidth)
        result.paddingStart = min(0f, italic) * emScale
        result.paddingEnd = max(0f, italic) * emScale
        return result
    }

    override fun charWidth(char: Char, enforceSameWidthDigits: Boolean): Float {
        val g = if (char.isDigit() && enforceSameWidthDigits) {
            data.maxWidthDigit
        } else {
            data.glyphMap[char]
        }
        if (g == null) {
            return 0f
        }
        return g.advance * emScale
    }

    override fun charHeight(char: Char): Float {
        val g = data.glyphMap[char] ?: return 0f
        return (g.planeBounds.top - g.planeBounds.bottom) * emScale
    }

    override fun derive(sizePts: Float) = MsdfFont(data, sizePts, italic, weight, cutoff, glowColor)

    fun copy(
        sizePts: Float = this.sizePts,
        italic: Float = this.italic,
        weight: Float = this.weight,
        cutoff: Float = this.cutoff,
        glowColor: Color? = this.glowColor
    ) = MsdfFont(data, sizePts, italic, weight, cutoff, glowColor)

    override fun toString(): String {
        return "MsdfFont { name: ${data.meta.name}, info: ${data.meta.atlas} }"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MsdfFont

        if (data != other.data) return false
        if (sizePts != other.sizePts) return false
        if (italic != other.italic) return false
        if (weight != other.weight) return false
        if (cutoff != other.cutoff) return false
        if (glowColor != other.glowColor) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + sizePts.hashCode()
        result = 31 * result + italic.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + cutoff.hashCode()
        result = 31 * result + (glowColor?.hashCode() ?: 0)
        result = 31 * result + scale.hashCode()
        return result
    }

    companion object {
        const val WEIGHT_EXTRA_LIGHT = -0.09f
        const val WEIGHT_LIGHT = -0.06f
        const val WEIGHT_REGULAR = 0.0f
        const val WEIGHT_BOLD = 0.1f
        const val WEIGHT_EXTRA_BOLD = 0.15f

        const val ITALIC_NONE = 0.0f
        const val ITALIC_STD = 0.25f

        const val CUTOFF_SOLID = 1f
        const val CUTOFF_OUTLINED_THICK = 0.15f
        const val CUTOFF_OUTLINED_THIN = 0.1f

        val MSDF_TEX_PROPS = TextureProps(generateMipMaps = false)

        val DEFAULT_FONT_DATA: MsdfFontData by lazy {
            val fontInfo = KoolSystem.config.defaultFont
            val msdfMap = Texture2d(
                props = MSDF_TEX_PROPS,
                name = "MsdfFont:${fontInfo.fontMeta.name}",
                loader = AsyncTextureLoader { Assets.loadTextureData("fonts/font-roboto-regular.png", MSDF_TEX_PROPS) }
            )
            MsdfFontData(msdfMap, fontInfo.fontMeta)
        }
        val DEFAULT_FONT: MsdfFont by lazy { MsdfFont(DEFAULT_FONT_DATA) }
    }
}

suspend fun MsdfFont(fontPath: String): MsdfFont = MsdfFont("${fontPath}.json", "${fontPath}.png")

suspend fun MsdfFont(metaPath: String, texturePath: String): MsdfFont {
    val json = Assets.loadBlobAsset(metaPath).decodeToString()
    val meta = Json.Default.decodeFromString(MsdfMeta.serializer(), json)
    return MsdfFont(MsdfFontInfo(meta, texturePath))
}

suspend fun MsdfFont(fontInfo: MsdfFontInfo): MsdfFont {
    val fontData = MsdfFontData(Assets.loadTexture2d(fontInfo.texturePath, MsdfFont.MSDF_TEX_PROPS), fontInfo.fontMeta)
    return MsdfFont(fontData)
}

data class MsdfFontInfo(val fontMeta: MsdfMeta, val texturePath: String)

class MsdfFontData(val map: Texture2d, val meta: MsdfMeta) {
    val glyphMap = meta.glyphs.associateBy { it.unicode.toChar() }
    val kerning = meta.kerning.associate { (it.unicode1 shl 16) or it.unicode2 to it.advance }

    val maxWidthDigit: MsdfGlyph? = ('0' ..'9').mapNotNull { glyphMap[it] }.maxByOrNull { it.advance }
}

@Serializable
data class MsdfMeta(
    val atlas: MsdfAtlasInfo,
    val name: String,
    val metrics: MsdfMetrics,
    val glyphs: List<MsdfGlyph>,
    val kerning: List<MsdfKerning>
)

@Serializable
data class MsdfAtlasInfo(
    val type: String,
    val distanceRange: Float,
    val size: Float,
    val width: Int,
    val height: Int,
    val yOrigin: String
)

@Serializable
data class MsdfMetrics(
    val emSize: Float,
    val lineHeight: Float,
    val ascender: Float,
    val descender: Float,
    val underlineY: Float,
    val underlineThickness: Float
)

@Serializable
data class MsdfGlyph(
    val unicode: Int,
    val advance: Float,
    val planeBounds: MsdfRect = MsdfRect(0f, 0f, 0f, 0f),
    val atlasBounds: MsdfRect = MsdfRect(0f, 0f, 0f, 0f),
) {
    fun isEmpty(): Boolean = planeBounds.left == planeBounds.right
}

@Serializable
data class MsdfRect(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float
)

@Serializable
data class MsdfKerning(
    val unicode1: Int,
    val unicode2: Int,
    val advance: Float
)
