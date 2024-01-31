package de.fabmax.kool.pipeline.backend.gl

import de.fabmax.kool.pipeline.*
import de.fabmax.kool.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MappedUniform {
    fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean
}

class MappedUbo(val ubo: BindGroupData.UniformBufferBindingData, val gpuBuffer: BufferResource, val backend: RenderBackendGl) : MappedUniform {
    val gl: GlApi get() = backend.gl

    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        if (ubo.getAndClearDirtyFlag()) {
            gpuBuffer.setData(ubo.buffer, gl.DYNAMIC_DRAW)
        }
        gl.bindBufferBase(gl.UNIFORM_BUFFER, bindCtx.location(ubo.layout.bindingIndex), gpuBuffer.buffer)
        return true
    }
}

class MappedUboCompat(val ubo: BindGroupData.UniformBufferBindingData, val gl: GlApi) : MappedUniform {
    private val floatBuffers = buildList {
        ubo.layout.uniforms.forEach {
            val bufferSize = if (it.isArray) {
                when (it.type) {
                    GpuType.FLOAT1 ->  1 * it.arraySize
                    GpuType.FLOAT2 ->  2 * it.arraySize
                    GpuType.FLOAT3 ->  3 * it.arraySize
                    GpuType.FLOAT4 ->  4 * it.arraySize
                    GpuType.MAT3   ->  9 * it.arraySize
                    GpuType.MAT4   -> 16 * it.arraySize
                    else           ->  1
                }
            } else {
                when (it.type) {
                    GpuType.MAT3   ->  9
                    GpuType.MAT4   -> 16
                    else           ->  1
                }
            }
            add(Float32Buffer(bufferSize))
        }
    }

    private val intBuffers = buildList {
        ubo.layout.uniforms.forEach {
            val bufferSize = if (it.isArray) {
                when (it.type) {
                    GpuType.INT1 -> 1 * it.arraySize
                    GpuType.INT2 -> 2 * it.arraySize
                    GpuType.INT3 -> 3 * it.arraySize
                    GpuType.INT4 -> 4 * it.arraySize
                    else         -> 1
                }
            } else { 1 }
            add(Int32Buffer(bufferSize))
        }
    }

    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        ubo.layout.uniforms.forEachIndexed { i, uniform ->
            val loc = bindCtx.locations(ubo.layout.bindingIndex)[i]
            val buf = ubo.buffer
            val pos = ubo.layout.layout.uniformPositions[uniform.name]!!.byteIndex

            if (uniform.isArray) {
                when (uniform.type) {
                    GpuType.FLOAT1 -> gl.uniform1fv(loc, floatBuffers[i].copyPadded(buf, pos, 1, uniform.arraySize))
                    GpuType.FLOAT2 -> gl.uniform2fv(loc, floatBuffers[i].copyPadded(buf, pos, 2, uniform.arraySize))
                    GpuType.FLOAT3 -> gl.uniform3fv(loc, floatBuffers[i].copyPadded(buf, pos, 3, uniform.arraySize))
                    GpuType.FLOAT4 -> gl.uniform4fv(loc, floatBuffers[i].copyPadded(buf, pos, 4, uniform.arraySize))
                    GpuType.INT1 -> gl.uniform1iv(loc, intBuffers[i].copyPadded(buf, pos, 1, uniform.arraySize))
                    GpuType.INT2 -> gl.uniform2iv(loc, intBuffers[i].copyPadded(buf, pos, 2, uniform.arraySize))
                    GpuType.INT3 -> gl.uniform3iv(loc, intBuffers[i].copyPadded(buf, pos, 3, uniform.arraySize))
                    GpuType.INT4 -> gl.uniform4iv(loc, intBuffers[i].copyPadded(buf, pos, 4, uniform.arraySize))
                    GpuType.MAT2 -> gl.uniformMatrix2fv(loc, floatBuffers[i].copyPadded(buf, pos, 2, 2 * uniform.arraySize))
                    GpuType.MAT3 -> gl.uniformMatrix3fv(loc, floatBuffers[i].copyPadded(buf, pos, 3, 3 * uniform.arraySize))
                    GpuType.MAT4 -> gl.uniformMatrix4fv(loc, floatBuffers[i].copyPadded(buf, pos, 4, 4 * uniform.arraySize))
                }
            } else {
                when (uniform.type) {
                    GpuType.FLOAT1 -> gl.uniform1f(loc, buf.getFloat32(pos))
                    GpuType.FLOAT2 -> gl.uniform2f(loc, buf.getFloat32(pos), buf.getFloat32(pos + 4))
                    GpuType.FLOAT3 -> gl.uniform3f(loc, buf.getFloat32(pos), buf.getFloat32(pos + 4), buf.getFloat32(pos + 8))
                    GpuType.FLOAT4 -> gl.uniform4f(loc, buf.getFloat32(pos), buf.getFloat32(pos + 4), buf.getFloat32(pos + 8), buf.getFloat32(pos + 12))
                    GpuType.INT1 -> gl.uniform1i(loc, buf.getInt32(pos))
                    GpuType.INT2 -> gl.uniform2i(loc, buf.getInt32(pos), buf.getInt32(pos + 4))
                    GpuType.INT3 -> gl.uniform3i(loc, buf.getInt32(pos), buf.getInt32(pos + 4), buf.getInt32(pos + 8))
                    GpuType.INT4 -> gl.uniform4i(loc, buf.getInt32(pos), buf.getInt32(pos + 4), buf.getInt32(pos + 8), buf.getInt32(pos + 12))
                    GpuType.MAT2 -> gl.uniformMatrix2fv(loc, floatBuffers[i].copyPadded(buf, pos, 2, 2))
                    GpuType.MAT3 -> gl.uniformMatrix3fv(loc, floatBuffers[i].copyPadded(buf, pos, 3, 3))
                    GpuType.MAT4 -> gl.uniformMatrix4fv(loc, floatBuffers[i].copyPadded(buf, pos, 4, 4))
                }
            }
        }
        return true
    }

    private fun Float32Buffer.copyPadded(src: MixedBuffer, start: Int, values: Int, count: Int = 1): Float32Buffer {
        var pSrc = start
        var pDst = 0
        for (i in 0 until count) {
            for (iVal in 0 until values) {
                set(pDst++, src.getFloat32(pSrc))
                pSrc += 4
            }
            pSrc += 4 * (4 - values)
        }
        return this
    }

    private fun Int32Buffer.copyPadded(src: MixedBuffer, start: Int, values: Int, count: Int = 1): Int32Buffer {
        var pSrc = start
        var pDst = 0
        for (i in 0 until count) {
            for (iVal in 0 until values) {
                set(pDst++, src.getInt32(pSrc))
                pSrc += 4
            }
            pSrc += 4 * (4 - values)
        }
        return this
    }
}

sealed class MappedUniformTex(val target: Int, val backend: RenderBackendGl) : MappedUniform {
    protected val gl = backend.gl

    private fun checkLoadingState(texture: Texture, texUnit: Int): Boolean {
        texture.checkIsNotReleased()

        if (texture.loadingState == Texture.LoadingState.NOT_LOADED) {
            when (texture.loader) {
                is AsyncTextureLoader -> {
                    texture.loadingState = Texture.LoadingState.LOADING
                    CoroutineScope(Dispatchers.RenderLoop).launch {
                        val texData = texture.loader.loadTextureDataAsync().await()
                        texture.loadedTexture = getLoadedTex(texData, texture, backend)
                        texture.loadingState = Texture.LoadingState.LOADED
                    }
                }
                is SyncTextureLoader -> {
                    val data = texture.loader.loadTextureDataSync()
                    texture.loadedTexture = getLoadedTex(data, texture, backend)
                    texture.loadingState = Texture.LoadingState.LOADED
                }
                is BufferedTextureLoader -> {
                    texture.loadedTexture = getLoadedTex(texture.loader.data, texture, backend)
                    texture.loadingState = Texture.LoadingState.LOADED
                }
                else -> {
                    // loader is null
                    texture.loadingState = Texture.LoadingState.LOADING_FAILED
                }
            }
        }
        if (texture.loadingState == Texture.LoadingState.LOADED) {
            val tex = texture.loadedTexture as LoadedTextureGl
            gl.activeTexture(gl.TEXTURE0 + texUnit)
            tex.bind()
            tex.applySamplerSettings(null)
            return true
        }

        return false
    }

    protected fun setTexture(texture: Texture?, bindingIndex: Int, bindCtx: CompiledShader.UniformBindContext): Boolean {
        val texUnit = bindCtx.nextTexUnit++
        if (texture != null && checkLoadingState(texture, texUnit)) {
            gl.uniform1i(bindCtx.locations(bindingIndex)[0], texUnit)
            return true
        }
        return false
    }

    companion object {
        private val loadedTextures = mutableMapOf<TextureData, LoadedTextureGl>()

        internal fun getLoadedTex(texData: TextureData, texture: Texture, backend: RenderBackendGl): LoadedTextureGl {
            loadedTextures.values.removeAll { it.isReleased }
            return loadedTextures.getOrPut(texData) {
                val loaded = when (texture) {
                    is StorageTexture1d -> TextureLoaderGl.loadTexture1d(texture, texData, backend)
                    is Texture1d -> TextureLoaderGl.loadTexture1dCompat(texture, texData, backend)
                    is Texture2d -> TextureLoaderGl.loadTexture2d(texture, texData, backend)
                    is Texture3d -> TextureLoaderGl.loadTexture3d(texture, texData, backend)
                    is TextureCube -> TextureLoaderGl.loadTextureCube(texture, texData as TextureDataCube, backend)
                    else -> throw IllegalArgumentException("Unsupported texture type")
                }
                loaded
            }
        }
    }
}

class MappedUniformTex1d(private val sampler1d: BindGroupData.Texture1dBindingData, backend: RenderBackendGl) :
    MappedUniformTex(backend.gl.TEXTURE_2D, backend)
{
    // 1d texture internally uses a 2d texture to be compatible with glsl version 300 es
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        return setTexture(sampler1d.texture, sampler1d.layout.bindingIndex, bindCtx)
    }
}

class MappedUniformTex2d(private val sampler2d: BindGroupData.Texture2dBindingData, backend: RenderBackendGl) :
    MappedUniformTex(backend.gl.TEXTURE_2D, backend)
{
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        return setTexture(sampler2d.texture, sampler2d.layout.bindingIndex, bindCtx)
    }
}

class MappedUniformTex3d(private val sampler3d: BindGroupData.Texture3dBindingData, backend: RenderBackendGl) :
    MappedUniformTex(backend.gl.TEXTURE_3D, backend)
{
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        return setTexture(sampler3d.texture, sampler3d.layout.bindingIndex, bindCtx)
    }
}

class MappedUniformTexCube(private val samplerCube: BindGroupData.TextureCubeBindingData, backend: RenderBackendGl) :
    MappedUniformTex(backend.gl.TEXTURE_CUBE_MAP, backend)
{
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        return setTexture(samplerCube.texture, samplerCube.layout.bindingIndex, bindCtx)
    }
}

sealed class MappedUniformStorage(
    private val storage: StorageTextureLayout,
    private val backend: RenderBackendGl
) : MappedUniform {
    protected val gl = backend.gl

    protected fun bindStorageTex(storageTex: Texture, storageUnit: Int): Boolean {
        val loadedTex = checkLoadingState(storageTex)
        gl.bindImageTexture(
            unit = storageUnit,
            texture = loadedTex.glTexture,
            level = storage.level,
            layered = false,
            layer = 0,
            access = storage.accessType.glAccessType(gl),
            format = storage.format.glFormat(gl)
        )
        return true
    }

    private fun checkLoadingState(storageTex: Texture): LoadedTextureGl {
        if (storageTex.loadingState == Texture.LoadingState.NOT_LOADED) {
            val loader = storageTex.loader as BufferedTextureLoader
            storageTex.loadedTexture = MappedUniformTex.getLoadedTex(loader.data, storageTex, backend).also {
                it.bind()
                it.applySamplerSettings(storageTex.props.defaultSamplerSettings)
            }
            storageTex.loadingState = Texture.LoadingState.LOADED
        }
        return storageTex.loadedTexture as LoadedTextureGl
    }
}

class MappedUniformStorage1d(
    private val storage: BindGroupData.StorageTexture1dBindingData,
    backend: RenderBackendGl
) : MappedUniformStorage(storage.layout, backend) {
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        val storageTex = storage.storageTexture ?: return false
        val storageUnit = bindCtx.location(storage.layout.bindingIndex)
        bindStorageTex(storageTex, storageUnit)
        return true
    }
}

class MappedUniformStorage2d(
    private val storage: BindGroupData.StorageTexture2dBindingData,
    backend: RenderBackendGl
) : MappedUniformStorage(storage.layout, backend) {
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        val storageTex = storage.storageTexture ?: return false
        val storageUnit = bindCtx.location(storage.layout.bindingIndex)
        bindStorageTex(storageTex, storageUnit)
        return true
    }
}

class MappedUniformStorage3d(
    private val storage: BindGroupData.StorageTexture3dBindingData,
    backend: RenderBackendGl
) : MappedUniformStorage(storage.layout, backend) {
    override fun setUniform(bindCtx: CompiledShader.UniformBindContext): Boolean {
        val storageTex = storage.storageTexture ?: return false
        val storageUnit = bindCtx.location(storage.layout.bindingIndex)
        bindStorageTex(storageTex, storageUnit)
        return true
    }
}