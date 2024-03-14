package de.fabmax.kool

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.util.MsdfFontInfo
import de.fabmax.kool.util.MsdfMeta
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

data class KoolConfigJvm(
    /**
     * Default asset loader used by [Assets] to load textures, models, etc.
     */
    override val defaultAssetLoader: AssetLoader = NativeAssetLoader("./assets"),

    /**
     * Default font used by UI elements.
     */
    override val defaultFont: MsdfFontInfo = DEFAULT_MSDF_FONT_INFO,

    /**
     * Alternative base path used by [Assets] to look for assets using the classloader. This can be used to place
     * assets into the resources directory under /src/commonMain/resources/[classloaderAssetPath]. The classloader
     * asset path is preferred by the [Assets] loader. If a requested asset is not found in the classloader resources
     * the regular file-system based asset path is used.
     */
    val classloaderAssetPath: String = "",

    val storageDir: String = "./.storage",
    val httpCacheDir: String = "./.httpCache",

    val renderBackend: Backend = Backend.OPEN_GL,
    val windowTitle: String = "Kool App",
    val windowSize: Vec2i = Vec2i(1600, 900),
    val isFullscreen: Boolean = false,
    val showWindowOnStart: Boolean = true,
    val monitor: Int = -1,
    val windowIcon: List<BufferedImage> = DEFAULT_ICON?.let { listOf(it) } ?: emptyList(),

    val isVsync: Boolean = true,
    val maxFrameRate: Int = 0,
    val windowNotFocusedFrameRate: Int = 0,
    val msaaSamples: Int = 8,
    val customTtfFonts: Map<String, String> = emptyMap(),

) : KoolConfig {
    companion object {
        val DEFAULT_ICON: BufferedImage? = try {
            KoolConfigJvm::class.java.classLoader.getResourceAsStream("icon.png").use {
                ImageIO.read(it)
            }
        } catch (e: Exception) { null }

        val DEFAULT_MSDF_FONT_INFO: MsdfFontInfo by lazy {
            KoolConfigJvm::class.java.classLoader
                .getResourceAsStream("fonts/font-roboto-regular.json").use {
                    checkNotNull(it) { "Failed to load \"fonts/font-roboto-regular.json\" from resources" }
                    val meta = Json.Default.decodeFromString(MsdfMeta.serializer(), it.readAllBytes().decodeToString())
                    MsdfFontInfo(meta, "fonts/font-roboto-regular.png")
                }
        }
    }

    enum class Backend(val displayName: String) {
        VULKAN("Vulkan"),
        OPEN_GL("OpenGL")
    }
}
