package de.fabmax.kool.pipeline.shadermodel

interface CodeGenerator {

    fun sampleTexture1d(texName: String, texCoords: String, lod: String? = null): String
    fun sampleTexture2d(texName: String, texCoords: String, lod: String? = null): String
    fun sampleTexture2dDepth(texName: String, texCoords: String): String
    fun sampleTexture3d(texName: String, texCoords: String, lod: String? = null): String
    fun sampleTextureCube(texName: String, texCoords: String, lod: String? = null): String

    fun appendFunction(name: String, glslCode: String)

    fun appendMain(glslCode: String)

    enum class ClipSpaceOrientation {
        Y_UP,
        Y_DOWN
    }
}