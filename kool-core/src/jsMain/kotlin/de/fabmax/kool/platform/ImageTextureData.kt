package de.fabmax.kool.platform

import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.TextureData
import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData

class ImageTextureData(image: HTMLImageElement, fmt: TexFormat?) : TextureData() {
    override val data = image

    init {
        if (!image.complete) {
            throw IllegalStateException("Image must be complete")
        }
        width = image.width
        height = image.height
        depth = 1

        fmt?.let { format = it }
    }
}

class ImageAtlasTextureData(image: HTMLImageElement, tilesX: Int, tilesY: Int, fmt: TexFormat?) : TextureData() {
    override val data: Array<ImageData>

    init {
        if (!image.complete) {
            throw IllegalStateException("Image must be complete")
        }
        width = image.width / tilesX
        height = image.height / tilesY
        depth = tilesX * tilesY
        fmt?.let { format = it }

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = width
        canvas.height = height
        val canvasCtx = canvas.getContext("2d") as CanvasRenderingContext2D

        data = Array(depth) { i ->
            val x = (i % tilesX).toDouble()
            val y = (i / tilesY).toDouble()
            val w = width.toDouble()
            val h = height.toDouble()
            canvasCtx.drawImage(image, x * w, y * h, w, h, 0.0, 0.0, w, h)
            canvasCtx.getImageData(0.0, 0.0, w, h)
        }
    }
}