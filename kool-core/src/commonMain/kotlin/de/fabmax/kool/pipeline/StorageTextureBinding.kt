package de.fabmax.kool.pipeline

import kotlin.reflect.KProperty

sealed class StorageTextureBinding<T: Texture?>(
    textureName: String,
    defaultTexture: T,
    shader: ShaderBase<*>
) : PipelineBinding(textureName, shader) {

    private var cache: T = defaultTexture

    fun get(): T {
        if (isValid) {
            bindGroupData?.let {
                cache = it.getFromData()
            }
        }
        return cache
    }

    fun set(value: T) {
        cache = value
        if (isValid) {
            bindGroupData?.setInData(value)
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    override fun setup(pipeline: PipelineBase) {
        super.setup(pipeline)

        pipeline.findBindingLayout<StorageTextureLayout> { it.name == bindingName }?.let { (group, tex) ->
            check(group.scope == BindGroupScope.PIPELINE) {
                "StorageTextureBinding only supports binding to BindGroupData of scope ${BindGroupScope.PIPELINE}, but texture $bindingName has scope ${group.scope}."
            }
            bindGroup = group.group
            bindingIndex = tex.bindingIndex
            pipeline.pipelineData.setInData(cache)
        }
    }

    protected abstract fun BindGroupData.getFromData(): T
    protected abstract fun BindGroupData.setInData(texture: T)
}

class StorageTexture1dBinding(
    textureName: String,
    defaultTexture: StorageTexture1d?,
    shader: ShaderBase<*>
) : StorageTextureBinding<StorageTexture1d?>(textureName, defaultTexture, shader) {
    override fun BindGroupData.getFromData(): StorageTexture1d? {
        return storageTexture1dBindingData(bindingIndex).storageTexture
    }

    override fun BindGroupData.setInData(texture: StorageTexture1d?) {
        storageTexture1dBindingData(bindingIndex).storageTexture = texture
    }
}

class StorageTexture2dBinding(
    textureName: String,
    defaultTexture: StorageTexture2d?,
    shader: ShaderBase<*>
) : StorageTextureBinding<StorageTexture2d?>(textureName, defaultTexture, shader) {
    override fun BindGroupData.getFromData(): StorageTexture2d? {
        return storageTexture2dBindingData(bindingIndex).storageTexture
    }

    override fun BindGroupData.setInData(texture: StorageTexture2d?) {
        storageTexture2dBindingData(bindingIndex).storageTexture = texture
    }
}

class StorageTexture3dBinding(
    textureName: String,
    defaultTexture: StorageTexture3d?,
    shader: ShaderBase<*>
) : StorageTextureBinding<StorageTexture3d?>(textureName, defaultTexture, shader) {
    override fun BindGroupData.getFromData(): StorageTexture3d? {
        return storageTexture3dBindingData(bindingIndex).storageTexture
    }

    override fun BindGroupData.setInData(texture: StorageTexture3d?) {
        storageTexture3dBindingData(bindingIndex).storageTexture = texture
    }
}