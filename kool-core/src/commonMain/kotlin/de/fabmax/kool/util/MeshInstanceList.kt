package de.fabmax.kool.util

import de.fabmax.kool.KoolException
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.GlslType

class MeshInstanceList(val instanceAttributes: List<Attribute>, val maxInstances: Int = 1000) {

    /**
     * Vertex attribute offsets in bytes.
     */
    val attributeOffsets: Map<Attribute, Int>

    /**
     * Number of floats per instance. E.g. an instance containing only a model matrix consists of 16 floats.
     */
    val instanceSizeF: Int

    /**
     * Number of bytes per instance. E.g. an instance containing only a model matrix consists of 16 * 4 = 64 bytes.
     */
    val strideBytesF: Int

    /**
     * Expected usage of data in this instance list: STATIC if attributes are expected to change very infrequently /
     * never, DYNAMIC if they will be updated often.
     */
    var usage = Usage.DYNAMIC

    /**
     * Number of instances.
     */
    var numInstances = 0

    var dataF: Float32Buffer

    var hasChanged = true

    init {
        var strideF = 0
        val offsets = mutableMapOf<Attribute, Int>()
        for (attrib in instanceAttributes) {
            if (attrib.type.isInt) {
                throw IllegalArgumentException("For now only float attributes are supported")
            } else {
                offsets[attrib] = strideF
                strideF += attrib.type.byteSize
            }
        }

        attributeOffsets = offsets
        instanceSizeF = strideF / 4
        strideBytesF = strideF
        dataF = createFloat32Buffer(strideF * maxInstances)
    }

    fun checkBufferSize(reqSpace: Int = 1) {
        if (numInstances + reqSpace > maxInstances) {
            throw KoolException("Maximum number of instances exceeded, create with increased maxInstances")
        }
    }

    inline fun addInstance(block: Float32Buffer.() -> Unit) {
        checkBufferSize()
        val szBefore = dataF.position
        dataF.block()
        val growSz = dataF.position - szBefore
        if (growSz != instanceSizeF) {
            throw IllegalStateException("Expected data to grow by $instanceSizeF elements, instead it grew by $growSz")
        }
        numInstances++
        hasChanged = true
    }

    fun clear() {
        numInstances = 0
        dataF.position = 0
        dataF.limit = dataF.capacity
        hasChanged = true
    }

    companion object {
        val MODEL_MAT = Attribute("attrib_model_mat", GlslType.MAT_4F)
    }
}