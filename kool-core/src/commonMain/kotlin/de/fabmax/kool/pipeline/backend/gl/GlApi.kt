package de.fabmax.kool.pipeline.backend.gl

import de.fabmax.kool.pipeline.TextureData
import de.fabmax.kool.util.*
import kotlin.jvm.JvmInline

@JvmInline
value class GlBuffer(val handle: Int)
@JvmInline
value class GlFramebuffer(val handle: Int)
@JvmInline
value class GlProgram(val handle: Int)
@JvmInline
value class GlRenderbuffer(val handle: Int)
@JvmInline
value class GlShader(val handle: Int)
@JvmInline
value class GlTexture(val handle: Int)

interface GlApi {
    val ARRAY_BUFFER: Int
    val BACK: Int
    val BLEND: Int
    val CLAMP_TO_EDGE: Int
    val COLOR: Int
    val COLOR_ATTACHMENT0: Int
    val COLOR_BUFFER_BIT: Int
    val COMPARE_REF_TO_TEXTURE: Int
    val COMPILE_STATUS: Int
    val CULL_FACE: Int
    val DEPTH_ATTACHMENT: Int
    val DEPTH_BUFFER_BIT: Int
    val DEPTH_COMPONENT24: Int
    val DEPTH_COMPONENT32F: Int
    val DEPTH_TEST: Int
    val DYNAMIC_DRAW: Int
    val ELEMENT_ARRAY_BUFFER: Int
    val FRAGMENT_SHADER: Int
    val FRAMEBUFFER: Int
    val FRONT: Int
    val INVALID_INDEX: Int
    val LINEAR: Int
    val LINEAR_MIPMAP_LINEAR: Int
    val LINES: Int
    val LINK_STATUS: Int
    val MIRRORED_REPEAT: Int
    val NEAREST: Int
    val NEAREST_MIPMAP_NEAREST: Int
    val NONE: Int
    val ONE: Int
    val ONE_MINUS_SRC_ALPHA: Int
    val POINTS: Int
    val RENDERBUFFER: Int
    val REPEAT: Int
    val SRC_ALPHA: Int
    val STATIC_DRAW: Int
    val TEXTURE_2D: Int
    val TEXTURE_3D: Int
    val TEXTURE_COMPARE_MODE: Int
    val TEXTURE_COMPARE_FUNC: Int
    val TEXTURE_CUBE_MAP: Int
    val TEXTURE_CUBE_MAP_POSITIVE_X: Int
    val TEXTURE_CUBE_MAP_NEGATIVE_X: Int
    val TEXTURE_CUBE_MAP_POSITIVE_Y: Int
    val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int
    val TEXTURE_CUBE_MAP_POSITIVE_Z: Int
    val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int
    val TEXTURE_MAG_FILTER: Int
    val TEXTURE_MIN_FILTER: Int
    val TEXTURE_WRAP_R: Int
    val TEXTURE_WRAP_S: Int
    val TEXTURE_WRAP_T: Int
    val TEXTURE0: Int
    val TRIANGLES: Int
    val TRUE: Int
    val UNIFORM_BLOCK_DATA_SIZE: Int
    val UNIFORM_BUFFER: Int
    val UNIFORM_OFFSET: Int
    val VERTEX_SHADER: Int

    val INT: Int
    val FLOAT: Int
    val UNSIGNED_BYTE: Int
    val UNSIGNED_INT: Int

    val RED: Int
    val RG: Int
    val RGB: Int
    val RGBA: Int

    val R8: Int
    val RG8: Int
    val RGB8: Int
    val RGBA8: Int
    val R16F: Int
    val RG16F: Int
    val RGB16F: Int
    val RGBA16F: Int
    val R32F: Int
    val RG32F: Int
    val RGB32F: Int
    val RGBA32F: Int

    val ALWAYS: Int
    val NEVER: Int
    val LESS: Int
    val LEQUAL: Int
    val GREATER: Int
    val GEQUAL: Int
    val EQUAL: Int
    val NOTEQUAL: Int

    val NULL_BUFFER: GlBuffer
    val NULL_FRAMEBUFFER: GlFramebuffer
    val NULL_TEXTURE: GlTexture

    fun activeTexture(texture: Int)
    fun attachShader(program: GlProgram, shader: GlShader)
    fun bindBuffer(target: Int, buffer: GlBuffer)
    fun bindBufferBase(target: Int, index: Int, buffer: GlBuffer)
    fun bindFramebuffer(target: Int, framebuffer: GlFramebuffer)
    fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbuffer)
    fun bindTexture(target: Int, texture: GlTexture)
    fun blendFunc(sFactor: Int, dFactor: Int)
    fun bufferData(target: Int, buffer: Uint8Buffer, usage: Int)
    fun bufferData(target: Int, buffer: Uint16Buffer, usage: Int)
    fun bufferData(target: Int, buffer: Int32Buffer, usage: Int)
    fun bufferData(target: Int, buffer: Float32Buffer, usage: Int)
    fun bufferData(target: Int, buffer: MixedBuffer, usage: Int)
    fun clear(mask: Int)
    fun clearBufferfv(buffer: Int, drawBuffer: Int, values: Float32Buffer)
    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun createBuffer(): GlBuffer
    fun createFramebuffer(): GlFramebuffer
    fun createProgram(): GlProgram
    fun createRenderbuffer(): GlRenderbuffer
    fun createShader(type: Int): GlShader
    fun createTexture(): GlTexture
    fun compileShader(shader: GlShader)
    fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int)
    fun cullFace(mode: Int)
    fun deleteBuffer(buffer: GlBuffer)
    fun deleteFramebuffer(framebuffer: GlFramebuffer)
    fun deleteProgram(program: GlProgram)
    fun deleteRenderbuffer(renderbuffer: GlRenderbuffer)
    fun deleteShader(shader: GlShader)
    fun deleteTexture(texture: GlTexture)
    fun depthFunc(func: Int)
    fun depthMask(flag: Boolean)
    fun disable(cap: Int)
    fun disableVertexAttribArray(index: Int)
    fun drawBuffers(buffers: IntArray)
    fun drawElements(mode: Int, count: Int, type: Int)
    fun drawElementsInstanced(mode: Int, count: Int, type: Int, instanceCount: Int)
    fun enable(cap: Int)
    fun enableVertexAttribArray(index: Int)
    fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: GlRenderbuffer)
    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: GlTexture, level: Int)
    fun generateMipmap(target: Int)
    fun getActiveUniformBlockParameter(program: GlProgram, uniformBlockIndex: Int, pName: Int): Int
    fun getActiveUniforms(program: GlProgram, uniformIndices: IntArray, pName: Int): IntArray
    fun getProgramParameter(program: GlProgram, param: Int): Int
    fun getProgramInfoLog(program: GlProgram): String
    fun getShaderInfoLog(shader: GlShader): String
    fun getShaderParameter(shader: GlShader, param: Int): Int
    fun getUniformBlockIndex(program: GlProgram, uniformBlockName: String): Int
    fun getUniformIndices(program: GlProgram, names: Array<String>): IntArray
    fun getUniformLocation(program: GlProgram, uniformName: String): Int
    fun lineWidth(width: Float)
    fun linkProgram(program: GlProgram)
    fun readBuffer(src: Int)
    fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
    fun shaderSource(shader: GlShader, source: String)
    fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?)
    fun texImage2d(target: Int, data: TextureData)
    fun texImage3d(target: Int, data: TextureData)
    fun texParameteri(target: Int, pName: Int, param: Int)
    fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun uniformBlockBinding(program: GlProgram, uniformBlockIndex: Int, uniformBlockBinding: Int)
    fun useProgram(program: GlProgram)
    fun uniform1f(location: Int, x: Float)
    fun uniform2f(location: Int, x: Float, y: Float)
    fun uniform3f(location: Int, x: Float, y: Float, z: Float)
    fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float)
    fun uniform1fv(location: Int, values: Float32Buffer)
    fun uniform2fv(location: Int, values: Float32Buffer)
    fun uniform3fv(location: Int, values: Float32Buffer)
    fun uniform4fv(location: Int, values: Float32Buffer)
    fun uniform1i(location: Int, x: Int)
    fun uniform2i(location: Int, x: Int, y: Int)
    fun uniform3i(location: Int, x: Int, y: Int, z: Int)
    fun uniform4i(location: Int, x: Int, y: Int, z: Int, w: Int)
    fun uniform1iv(location: Int, values: Int32Buffer)
    fun uniform2iv(location: Int, values: Int32Buffer)
    fun uniform3iv(location: Int, values: Int32Buffer)
    fun uniform4iv(location: Int, values: Int32Buffer)
    fun uniformMatrix3fv(location: Int, values: Float32Buffer)
    fun uniformMatrix4fv(location: Int, values: Float32Buffer)
    fun vertexAttribDivisor(index: Int, divisor: Int)
    fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun viewport(x: Int, y: Int, width: Int, height: Int)


}