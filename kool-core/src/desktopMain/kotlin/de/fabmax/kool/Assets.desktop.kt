package de.fabmax.kool

import com.github.weisj.jsvg.parser.SVGLoader
import de.fabmax.kool.modules.filesystem.FileSystemAssetLoader
import de.fabmax.kool.modules.filesystem.FileSystemAssetLoaderDesktop
import de.fabmax.kool.modules.filesystem.FileSystemDirectory
import de.fabmax.kool.pipeline.TextureData
import de.fabmax.kool.pipeline.TextureData2d
import de.fabmax.kool.pipeline.TextureProps
import de.fabmax.kool.platform.FontMapGenerator
import de.fabmax.kool.platform.HttpCache
import de.fabmax.kool.platform.ImageDecoder
import de.fabmax.kool.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.nfd.NFDFilterItem
import org.lwjgl.util.nfd.NativeFileDialog
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt

internal actual fun PlatformAssets(): PlatformAssets = PlatformAssetsImpl

actual fun fileSystemAssetLoader(baseDir: FileSystemDirectory): FileSystemAssetLoader = FileSystemAssetLoaderDesktop(baseDir)

object PlatformAssetsImpl : PlatformAssets {

    private const val MAX_GENERATED_TEX_WIDTH = 2048
    private const val MAX_GENERATED_TEX_HEIGHT = 2048

    private val fontGenerator = FontMapGenerator(MAX_GENERATED_TEX_WIDTH, MAX_GENERATED_TEX_HEIGHT)
    private var saveFileChooserPath = System.getProperty("user.home")
    private var loadFileChooserPath = System.getProperty("user.home")

    init {
        HttpCache.initCache(File(KoolSystem.configJvm.httpCacheDir))
        fontGenerator.loadCustomFonts(KoolSystem.configJvm.customTtfFonts)
    }

    override suspend fun waitForFonts() {
        // on JVM all fonts should be immediately available -> nothing to wait for
    }

    override fun createFontMapData(font: AtlasFont, fontScale: Float, outMetrics: MutableMap<Char, CharMetrics>): TextureData2d {
        return fontGenerator.createFontMapData(font, fontScale, outMetrics)
    }

    override suspend fun loadFileByUser(filterList: List<FileFilterItem>, multiSelect: Boolean): List<LoadableFile> {
        return openFileChooser(filterList, multiSelect).map { LoadableFileImpl(it) }
    }

    override suspend fun saveFileByUser(
        data: Uint8Buffer,
        defaultFileName: String?,
        filterList: List<FileFilterItem>,
        mimeType: String
    ): String? {
        return saveFileChooser(defaultFileName, filterList)?.let { saveFile ->
            saveFile.parentFile?.mkdirs()
            try {
                saveFile.writeBytes(data.toArray())
            } catch (e: IOException) {
                logE { "Saving file $saveFile failed: $e" }
                e.printStackTrace()
            }
            saveFile.absolutePath
        }
    }

    suspend fun openFileChooser(filterList: List<FileFilterItem> = emptyList(), multiSelect: Boolean = false): List<File> {
        // apparently file dialog functions need to be called from main thread
        // unfortunately, this means the main loop is blocked while the dialog is open
        return withContext(Dispatchers.RenderLoop) {
            memStack {
                var fileFilters: NFDFilterItem.Buffer? = null
                if (filterList.isNotEmpty()) {
                    fileFilters = NFDFilterItem.calloc(filterList.size)
                    filterList.forEachIndexed { i, filterItem ->
                        // make sure file extensions do not contain leading '.' and are separated by ',' with no space
                        val extensions = filterItem.fileExtensions
                            .split(',')
                            .joinToString(",") { it.trim().removePrefix(".") }
                        fileFilters[i].set(filterItem.name.toByteBuffer(), extensions.toByteBuffer())
                    }
                }

                val files = mutableListOf<File>()
                val outPath = callocPointer(1)
                if (multiSelect) {
                    val result = NativeFileDialog.NFD_OpenDialogMultiple(outPath, fileFilters, loadFileChooserPath)
                    if (result == NativeFileDialog.NFD_OKAY) {
                        val pathSetPtr = outPath.get(0)
                        val count = IntArray(1)
                        NativeFileDialog.NFD_PathSet_GetCount(pathSetPtr, count)
                        for (i in 0 until count[0]) {
                            if (NativeFileDialog.NFD_PathSet_GetPath(pathSetPtr, i, outPath) == NativeFileDialog.NFD_OKAY) {
                                files += File(outPath.getStringUTF8(0))
                                MemoryUtil.memFree(outPath)
                            }
                        }
                        NativeFileDialog.NFD_PathSet_Free(pathSetPtr)
                    }
                } else {
                    val result = NativeFileDialog.NFD_OpenDialog(outPath, fileFilters, loadFileChooserPath)
                    if (result == NativeFileDialog.NFD_OKAY) {
                        files += File(outPath.getStringUTF8(0))
                        MemoryUtil.memFree(outPath)
                    }
                }

                if (files.isNotEmpty()) {
                    loadFileChooserPath = files.first().parent
                }
                files
            }
        }
    }

    suspend fun saveFileChooser(defaultFileName: String? = null, filterList: List<FileFilterItem> = emptyList()): File? {
        // apparently file dialog functions need to be called from main thread
        // unfortunately, this means the main loop is blocked while the dialog is open
        return withContext(Dispatchers.RenderLoop) {
            memStack {
                val outPath = callocPointer(1)
                var fileFilters: NFDFilterItem.Buffer? = null
                if (filterList.isNotEmpty()) {
                    fileFilters = NFDFilterItem.calloc(filterList.size)
                    filterList.forEachIndexed { i, filterItem ->
                        fileFilters[i].set(filterItem.name.toByteBuffer(), filterItem.fileExtensions.toByteBuffer())
                    }
                }

                val result = NativeFileDialog.NFD_SaveDialog(outPath, fileFilters, saveFileChooserPath, defaultFileName)
                if (result == NativeFileDialog.NFD_OKAY) {
                    val file = File(outPath.stringUTF8)
                    saveFileChooserPath = file.parent
                    file
                } else {
                    null
                }
            }
        }
    }

    override suspend fun loadTextureDataFromBuffer(texData: Uint8Buffer, mimeType: String, props: TextureProps?): TextureData {
        return withContext(Dispatchers.IO) {
            readImageData(ByteArrayInputStream(texData.toArray()), mimeType, props)
        }
    }

    fun readImageData(inStream: InputStream, mimeType: String, props: TextureProps?): TextureData2d {
        return inStream.use {
            when (mimeType) {
                MimeType.IMAGE_SVG -> renderSvg(inStream, props)
                else -> ImageDecoder.loadImage(inStream, props)
            }
        }
    }

    private fun renderSvg(inStream: InputStream, props: TextureProps?): TextureData2d {
        val svgDoc = SVGLoader().load(inStream) ?: throw IllegalStateException("Failed loading SVG image")
        val size = svgDoc.size()
        val scaleX = if (props?.resolveSize != null) props.resolveSize.x / size.width else 1f
        val scaleY = if (props?.resolveSize != null) props.resolveSize.y / size.height else 1f

        val img = BufferedImage((size.width * scaleX).roundToInt(), (size.height * scaleY).roundToInt(), BufferedImage.TYPE_4BYTE_ABGR)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.scale(scaleX.toDouble(), scaleY.toDouble())
        svgDoc.render(null, g)
        g.dispose()

        return ImageDecoder.loadBufferedImage(img, props)
    }
}

actual suspend fun decodeDataUri(dataUri: String): Uint8Buffer {
    val dataIdx = dataUri.indexOf(";base64,") + 8
    return Uint8BufferImpl(Base64.getDecoder().decode(dataUri.substring(dataIdx)))
}