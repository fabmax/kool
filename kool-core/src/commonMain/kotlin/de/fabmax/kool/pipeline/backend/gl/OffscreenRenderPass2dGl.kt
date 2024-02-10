package de.fabmax.kool.pipeline.backend.gl

import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.backend.stats.OffscreenPassInfo
import de.fabmax.kool.util.logE

class OffscreenRenderPass2dGl(val parent: OffscreenRenderPass2d, val backend: RenderBackendGl) : OffscreenPass2dImpl {
    private val gl = backend.gl

    private val fbos = mutableListOf<GlFramebuffer>()
    private val rbos = mutableListOf<GlRenderbuffer>()

    internal val colorTextures = Array(parent.numColorAttachments) { gl.NULL_TEXTURE }
    internal var depthTexture = gl.NULL_TEXTURE

    private var isCreated = false

    private val resInfo = OffscreenPassInfo(parent)

    private val frameBufferSetter = QueueRenderer.FrameBufferSetter { viewIndex, mipLevel ->
        if (viewIndex == 0) {
            gl.bindFramebuffer(gl.FRAMEBUFFER, fbos[mipLevel])
        }
    }

    fun draw() {
        resInfo.sceneName = parent.parentScene?.name ?: "scene:<null>"
        if (!isCreated) {
            createBuffers()
        }

        val needsCopy = parent.copyTargetsColor.isNotEmpty()

        backend.queueRenderer.renderViews(parent, frameBufferSetter)

        if (parent.mipMode == RenderPass.MipMode.Generate) {
            for (i in colorTextures.indices) {
                gl.bindTexture(gl.TEXTURE_2D, colorTextures[i])
                gl.generateMipmap(gl.TEXTURE_2D)
            }
        }

        if (needsCopy) {
            if (gl.capabilities.canFastCopyTextures) {
                // use fast texture copy method, requires OpenGL 4.3 or higher
                gl.copyTexturesFast(this)
            } else {
                for (mipLevel in 0 until parent.numTextureMipLevels) {
                    copyToTexturesCompat(mipLevel)
                }
            }
        }
        gl.bindFramebuffer(gl.FRAMEBUFFER, gl.DEFAULT_FRAMEBUFFER)
    }

    private fun copyToTexturesCompat(mipLevel: Int) {
        gl.bindFramebuffer(gl.FRAMEBUFFER, fbos[mipLevel])
        gl.readBuffer(gl.COLOR_ATTACHMENT0)
        for (i in parent.copyTargetsColor.indices) {
            val copyTarget = parent.copyTargetsColor[i]
            var width = copyTarget.loadedTexture?.width ?: 0
            var height = copyTarget.loadedTexture?.height ?: 0
            if (width != parent.width || height != parent.height) {
                // recreate target texture if size has changed
                copyTarget.loadedTexture?.release()
                copyTarget.createCopyTexColor(parent, backend)
                width = copyTarget.loadedTexture!!.width
                height = copyTarget.loadedTexture!!.height
            }
            width = width shr mipLevel
            height = height shr mipLevel
            val target = copyTarget.loadedTexture as LoadedTextureGl
            gl.bindTexture(gl.TEXTURE_2D, target.glTexture)
            gl.copyTexSubImage2D(gl.TEXTURE_2D, mipLevel, 0, 0, 0, 0, width, height)
        }
    }

    private fun deleteBuffers() {
        fbos.forEach { gl.deleteFramebuffer(it) }
        rbos.forEach { gl.deleteRenderbuffer(it) }
        fbos.clear()
        rbos.clear()

        parent.colorTextures.forEach { tex ->
            if (tex.loadingState == Texture.LoadingState.LOADED) {
                tex.dispose()
            }
        }
        parent.depthTexture?.let { tex ->
            if (tex.loadingState == Texture.LoadingState.LOADED) {
                tex.dispose()
            }
        }

        for (i in colorTextures.indices) { colorTextures[i] = gl.NULL_TEXTURE }
        depthTexture = gl.NULL_TEXTURE
        isCreated = false
    }

    override fun release() {
        deleteBuffers()
        resInfo.deleted()
    }

    override fun applySize(width: Int, height: Int) {
        deleteBuffers()
        createBuffers()
    }

    private fun createBuffers() {
        if (parent.colorAttachments is OffscreenRenderPass.ColorAttachmentTextures) {
            createColorTextures(parent.colorAttachments)
        }
        if (parent.depthAttachment is OffscreenRenderPass.DepthAttachmentTexture) {
            createDepthTexture(parent.depthAttachment)
        }

        for (mipLevel in 0 until parent.numRenderMipLevels) {
            val fbo = gl.createFramebuffer()
            fbos += fbo
            gl.bindFramebuffer(gl.FRAMEBUFFER, fbo)

            when (parent.colorAttachments) {
                OffscreenRenderPass.ColorAttachmentNone -> { }
                is OffscreenRenderPass.ColorAttachmentTextures -> attachColorTextures(mipLevel)
            }

            when (parent.depthAttachment) {
                OffscreenRenderPass.DepthAttachmentRender -> rbos += attachDepthRenderBuffer(mipLevel)
                OffscreenRenderPass.DepthAttachmentNone -> { }
                is OffscreenRenderPass.DepthAttachmentTexture -> attachDepthTexture(mipLevel)
            }

            if (gl.checkFramebufferStatus(gl.FRAMEBUFFER) != gl.FRAMEBUFFER_COMPLETE) {
                logE { "OffscreenRenderPass2dGl: Framebuffer incomplete: ${parent.name}, level: $mipLevel" }
            }
        }
        isCreated = true
    }

    private fun attachColorTextures(mipLevel: Int) {
        colorTextures.forEachIndexed { iAttachment, tex ->
            gl.framebufferTexture2D(
                target = gl.FRAMEBUFFER,
                attachment = gl.COLOR_ATTACHMENT0 + iAttachment,
                textarget = gl.TEXTURE_2D,
                texture = tex,
                level = mipLevel
            )
        }
        if (colorTextures.size > 1) {
            val attachments = IntArray(colorTextures.size) { gl.COLOR_ATTACHMENT0 + it }
            gl.drawBuffers(attachments)
        }
    }

    private fun attachDepthTexture(mipLevel: Int) {
        gl.framebufferTexture2D(
            target = gl.FRAMEBUFFER,
            attachment = gl.DEPTH_ATTACHMENT,
            textarget = gl.TEXTURE_2D,
            texture = depthTexture,
            level = mipLevel
        )
    }

    private fun attachDepthRenderBuffer(mipLevel: Int): GlRenderbuffer {
        val rbo = gl.createRenderbuffer()
        val mipWidth = parent.width shr mipLevel
        val mipHeight = parent.height shr mipLevel
        gl.bindRenderbuffer(gl.RENDERBUFFER, rbo)
        gl.renderbufferStorage(gl.RENDERBUFFER, gl.DEPTH_COMPONENT32F, mipWidth, mipHeight)
        gl.framebufferRenderbuffer(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.RENDERBUFFER, rbo)
        return rbo
    }

    private fun createColorTextures(colorAttachment: OffscreenRenderPass.ColorAttachmentTextures) {
        for (i in parent.colorTextures.indices) {
            val parentTex = parent.colorTextures[i]
            val format = colorAttachment.attachments[i].textureFormat
            val intFormat = format.glInternalFormat(gl)
            val width = parent.width
            val height = parent.height
            val mipLevels = parent.numTextureMipLevels

            val estSize = Texture.estimatedTexSize(width, height, 1, mipLevels, format.pxSize).toLong()
            val tex = LoadedTextureGl(gl.TEXTURE_2D, gl.createTexture(), backend, parentTex, estSize)
            tex.setSize(width, height, 1)
            tex.bind()
            tex.applySamplerSettings(parentTex.props.defaultSamplerSettings)
            gl.texStorage2D(gl.TEXTURE_2D, mipLevels, intFormat, width, height)

            colorTextures[i] = tex.glTexture
            parentTex.loadedTexture = tex
            parentTex.loadingState = Texture.LoadingState.LOADED
        }
    }

    private fun createDepthTexture(depthAttachment: OffscreenRenderPass.DepthAttachmentTexture) {
        check(depthAttachment.attachment.textureFormat == TexFormat.R_F32)

        val parentTex = parent.depthTexture!!
        val intFormat = gl.DEPTH_COMPONENT32F
        val width = parent.width
        val height = parent.height
        val mipLevels = parent.numTextureMipLevels

        val estSize = Texture.estimatedTexSize(width, height, 1, mipLevels, 4).toLong()
        val tex = LoadedTextureGl(gl.TEXTURE_2D, gl.createTexture(), backend, parentTex, estSize)
        tex.setSize(width, height, 1)
        tex.bind()
        tex.applySamplerSettings(parentTex.props.defaultSamplerSettings)
        gl.texStorage2D(gl.TEXTURE_2D, mipLevels, intFormat, width, height)

        depthTexture = tex.glTexture
        parentTex.loadedTexture = tex
        parentTex.loadingState = Texture.LoadingState.LOADED
    }

    private fun Texture2d.createCopyTexColor(pass: OffscreenRenderPass2d, backend: RenderBackendGl) {
        val gl = backend.gl
        val intFormat = props.format.glInternalFormat(gl)
        val width = pass.width
        val height = pass.height
        val mipLevels = pass.numTextureMipLevels

        val estSize = Texture.estimatedTexSize(width, height, 1, mipLevels, props.format.pxSize).toLong()
        val tex = LoadedTextureGl(gl.TEXTURE_2D, gl.createTexture(), backend, this, estSize)
        tex.setSize(width, height, 1)
        tex.bind()
        tex.applySamplerSettings(props.defaultSamplerSettings)
        gl.texStorage2D(gl.TEXTURE_2D, mipLevels, intFormat, width, height)
        loadedTexture = tex
        loadingState = Texture.LoadingState.LOADED
    }
}