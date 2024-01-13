package de.fabmax.kool.pipeline

import kotlin.reflect.KProperty

sealed class TextureBinding<T: Texture?>(
    textureName: String,
    defaultTexture: T,
    defaultSampler: SamplerSettings?,
    val shader: ShaderBase<*>
) : PipelineBinding(textureName) {

    private var cache: T = defaultTexture
    private var samplerCache: SamplerSettings? = defaultSampler

    fun get(): T {
        if (isValid) {
            shader.createdPipeline?.let {
                cache = it.bindGroupData[bindGroup].getFromData()
            }
        }
        return cache
    }

    fun set(value: T) {
        cache = value
        if (isValid) {
            shader.createdPipeline?.let {
                it.bindGroupData[bindGroup].setInData(value, samplerCache)
            }
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    override fun setup(pipeline: PipelineBase) {
        super.setup(pipeline)

        pipeline.bindGroupLayouts.find { group ->
            group.bindings.any { b -> b is TextureLayout && b.name == bindingName }
        }?.let { group ->
            val tex = group.bindings.first { b -> b.name == bindingName } as TextureLayout
            bindGroup = group.group
            bindingIndex = tex.bindingIndex
            pipeline.bindGroupData[bindGroup].setInData(cache, samplerCache)
        }
    }

    protected abstract fun BindGroupData.getFromData(): T
    protected abstract fun BindGroupData.setInData(texture: T, sampler: SamplerSettings?)
}

sealed class TextureArrayBinding<T: Texture?>(
    uniformName: String,
    arraySize: Int,
    defaultSampler: SamplerSettings?,
    private val cacheInitVal: T,
    val shader: ShaderBase<*>
) : PipelineBinding(uniformName) {

    private val cache = MutableList(arraySize) { cacheInitVal }
    private var samplerCache: SamplerSettings? = defaultSampler

    val arraySize: Int get() = cache.size

    operator fun get(index: Int): T {
        if (isValid) {
            shader.createdPipeline?.let {
                cache[index] = it.bindGroupData[bindGroup].getFromData(index)
            }
        }
        return cache[index]
    }

    operator fun set(index: Int, value: T) {
        cache[index] = value
        if (isValid) {
            shader.createdPipeline?.let {
                it.bindGroupData[bindGroup].setInData(index, value, samplerCache)
            }
        }
    }

    private fun resizeCache(newSize: Int) {
        while (newSize < cache.size) {
            cache.removeAt(cache.lastIndex)
        }
        while (newSize > cache.size) {
            cache.add(cacheInitVal)
        }
    }

    override fun setup(pipeline: PipelineBase) {
        super.setup(pipeline)

        pipeline.bindGroupLayouts.find { group ->
            group.bindings.any { b -> b is TextureLayout && b.name == bindingName }
        }?.let { group ->
            val tex = group.bindings.first { b -> b.name == bindingName } as TextureLayout
            resizeCache(tex.arraySize)
            bindGroup = group.group
            bindingIndex = tex.bindingIndex
            cache.forEachIndexed { i, cacheTex ->
                pipeline.bindGroupData[bindGroup].setInData(i, cacheTex, samplerCache)
            }
        }
    }

    protected abstract fun BindGroupData.getFromData(index: Int): T
    protected abstract fun BindGroupData.setInData(index: Int, texture: T, sampler: SamplerSettings?)
}

class Texture1dBinding(
    textureName: String,
    defaultTexture: Texture1d?,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureBinding<Texture1d?>(textureName, defaultTexture, defaultSampler, shader) {
    override fun BindGroupData.getFromData(): Texture1d? {
        return texture1dBindingData(bindingIndex).texture
    }

    override fun BindGroupData.setInData(texture: Texture1d?, sampler: SamplerSettings?) {
        val binding = texture1dBindingData(bindingIndex)
        binding.texture = texture
        binding.sampler = sampler
    }
}

class Texture2dBinding(
    textureName: String,
    defaultTexture: Texture2d?,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureBinding<Texture2d?>(textureName, defaultTexture, defaultSampler, shader) {
    override fun BindGroupData.getFromData(): Texture2d? {
        return texture2dBindingData(bindingIndex).texture
    }

    override fun BindGroupData.setInData(texture: Texture2d?, sampler: SamplerSettings?) {
        val binding = texture2dBindingData(bindingIndex)
        binding.texture = texture
        binding.sampler = sampler
    }
}

class Texture3dBinding(
    textureName: String,
    defaultTexture: Texture3d?,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureBinding<Texture3d?>(textureName, defaultTexture, defaultSampler, shader) {
    override fun BindGroupData.getFromData(): Texture3d? {
        return texture3dBindingData(bindingIndex).texture
    }

    override fun BindGroupData.setInData(texture: Texture3d?, sampler: SamplerSettings?) {
        val binding = texture3dBindingData(bindingIndex)
        binding.texture = texture
        binding.sampler = sampler
    }
}

class TextureCubeBinding(
    textureName: String,
    defaultTexture: TextureCube?,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureBinding<TextureCube?>(textureName, defaultTexture, defaultSampler, shader) {
    override fun BindGroupData.getFromData(): TextureCube? {
        return textureCubeBindingData(bindingIndex).texture
    }

    override fun BindGroupData.setInData(texture: TextureCube?, sampler: SamplerSettings?) {
        val binding = textureCubeBindingData(bindingIndex)
        binding.texture = texture
        binding.sampler = sampler
    }
}

class Texture1dArrayBinding(
    textureName: String,
    arraySize: Int,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureArrayBinding<Texture1d?>(textureName, arraySize, defaultSampler, null, shader) {
    override fun BindGroupData.getFromData(index: Int): Texture1d? {
        return texture1dBindingData(bindingIndex).textures[index]
    }

    override fun BindGroupData.setInData(index: Int, texture: Texture1d?, sampler: SamplerSettings?) {
        val binding = texture1dBindingData(bindingIndex)
        binding[index] = texture
        binding.sampler = sampler
    }
}

class Texture2dArrayBinding(
    textureName: String,
    arraySize: Int,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureArrayBinding<Texture2d?>(textureName, arraySize, defaultSampler, null, shader) {
    override fun BindGroupData.getFromData(index: Int): Texture2d? {
        return texture2dBindingData(bindingIndex).textures[index]
    }

    override fun BindGroupData.setInData(index: Int, texture: Texture2d?, sampler: SamplerSettings?) {
        val binding = texture2dBindingData(bindingIndex)
        binding[index] = texture
        binding.sampler = sampler
    }
}

class TextureCubeArrayBinding(
    textureName: String,
    arraySize: Int,
    defaultSampler: SamplerSettings?,
    shader: ShaderBase<*>
) : TextureArrayBinding<TextureCube?>(textureName, arraySize, defaultSampler, null, shader) {
    override fun BindGroupData.getFromData(index: Int): TextureCube? {
        return textureCubeBindingData(bindingIndex).textures[index]
    }

    override fun BindGroupData.setInData(index: Int, texture: TextureCube?, sampler: SamplerSettings?) {
        val binding = textureCubeBindingData(bindingIndex)
        binding[index] = texture
        binding.sampler = sampler
    }
}
