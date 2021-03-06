package de.fabmax.kool.platform

import de.fabmax.kool.math.clamp
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.TextureData2d
import de.fabmax.kool.util.*
import java.awt.Color
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.IOException
import kotlin.math.roundToInt

/**
 * @author fabmax
 */

internal class FontMapGenerator(val maxWidth: Int, val maxHeight: Int, props: Lwjgl3Context.InitProps, assetManager: JvmAssetManager) {

    private val canvas = BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB)
    private val clearColor = Color(0, 0, 0, 0)

    private val charMaps = mutableMapOf<FontProps, CharMap>()

    private val availableFamilies: Set<String>

    init {
        val families: MutableSet<String> = mutableSetOf()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        for (family in ge.availableFontFamilyNames) {
            families.add(family)
        }
        for (f in props.extraFonts) {
            try {
                val inStream = assetManager.openLocalStream(f)
                val localFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, inStream)
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(localFont)
                families.add(localFont.family)
                logI { "Registered font: $f -> ${localFont.family}" }
            } catch (e: IOException) {
                logW { "Font not found: $f" }
            }
        }

        availableFamilies = families
    }

    fun getCharMap(fontProps: FontProps): CharMap = charMaps.computeIfAbsent(fontProps) { generateCharMap(it) }

    private fun generateCharMap(fontProps: FontProps): CharMap {
        val g = canvas.graphics as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // clear canvas
        g.background = clearColor
        g.clearRect(0, 0, maxWidth, maxHeight)

        var style = java.awt.Font.PLAIN
        if (fontProps.style and Font.BOLD != 0) {
            style = java.awt.Font.BOLD
        }
        if (fontProps.style and Font.ITALIC != 0) {
            style += java.awt.Font.ITALIC
        }

        var family = java.awt.Font.SANS_SERIF
        val fams = fontProps.family.split(",")
        for (fam in fams) {
            val f = fam.trim().replace("\"", "")
            if (f == "sans-serif") {
                family = java.awt.Font.SANS_SERIF
                break
            } else if (f == "monospaced") {
                family = java.awt.Font.MONOSPACED
                break
            } else if (f in availableFamilies) {
                family = f
                break
            }
        }

        g.font = java.awt.Font(family, style, fontProps.sizePts.roundToInt())
        g.color = Color.BLACK

        val metrics: MutableMap<Char, CharMetrics> = mutableMapOf()
        val texHeight = makeMap(fontProps, g, metrics)
        val buffer = getCanvasAlphaData(maxWidth, texHeight)
        return CharMap(TextureData2d(buffer, maxWidth, texHeight, TexFormat.R), metrics, fontProps)
    }

    private fun getCanvasAlphaData(width: Int, height: Int): Uint8Buffer {
        val imgBuf = canvas.data.dataBuffer as DataBufferInt
        val pixels = imgBuf.bankData[0]
        val buffer = createUint8Buffer(width * height)
        for (i in 0 until width * height) {
            buffer.put((pixels[i] shr 24).toByte())
        }
        buffer.flip()
        return buffer
    }

    private fun makeMap(fontProps: FontProps, g: Graphics2D, map: MutableMap<Char, CharMetrics>): Int {
        val padding = (if (fontProps.style == Font.ITALIC) 3 else 6) * (fontProps.sizePts / 30f).clamp(1f, 3f).roundToInt()
        // line height above baseline
        val hab = (fontProps.sizePts * 1.1f).roundToInt()
        // line height below baseline
        val hbb = (fontProps.sizePts * 0.5f).roundToInt()
        // overall line height
        val height = (fontProps.sizePts * 1.6f).roundToInt()
        val fm = g.fontMetrics

        // first pixel is opaque
        g.fillRect(0, 0, 1, 1)

        var x = 1
        var y = hab
        for (c in fontProps.chars) {
            // super-ugly special treatment for 'j' which has a negative x-offset for most fonts
            if (c == 'j') {
                x += (fontProps.sizePts * 0.1f).roundToInt()
            }

            val charW = fm.charWidth(c)
            val paddedWidth = charW + padding * 2
            if (x + paddedWidth > maxWidth) {
                x = 0
                y += height + 10
                if (y + hbb > maxHeight) {
                    break
                }
            }

            val widthPx = charW.toFloat()
            val heightPx = height.toFloat()
            val metrics = CharMetrics()
            metrics.width = widthPx
            metrics.height = heightPx
            metrics.xOffset = 0f
            metrics.yBaseline = hab.toFloat()
            metrics.advance = metrics.width

            metrics.uvMin.set((x + padding).toFloat(), (y - hab).toFloat())
            metrics.uvMax.set((x + padding + widthPx), (y - hab).toFloat() + heightPx)
            map[c] = metrics

            g.drawString("$c", x + padding, y)
            x += paddedWidth
        }

        val texW = maxWidth
        val texH = nextPow2(y + hbb)

        for (cm in map.values) {
            cm.uvMin.x /= texW
            cm.uvMin.y /= texH
            cm.uvMax.x /= texW
            cm.uvMax.y /= texH
        }

        return texH
    }

    private fun nextPow2(value: Int): Int {
        var pow2 = 16
        while (pow2 < value && pow2 < maxHeight) {
            pow2 = pow2 shl 1
        }
        return pow2
    }
}
