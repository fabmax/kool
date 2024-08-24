package de.fabmax.kool.util

class LongHashBuilder {
    var hash: Long = 0L

    operator fun plusAssign(hash: LongHash) {
        this += hash.hash
    }

    operator fun plusAssign(long: Long) {
        hash = hash * 31L + long
    }

    operator fun plusAssign(int: Int) {
        hash = hash * 31L + int
    }

    operator fun plusAssign(byte: Byte) {
        hash = hash * 31L + byte
    }

    operator fun plusAssign(bool: Boolean) {
        hash = hash * 31L + bool.hashCode()
    }

    operator fun plusAssign(float: Float) {
        hash = hash * 31L + float.hashCode()
    }

    operator fun plusAssign(enum: Enum<*>) {
        hash = hash * 31L + enum.hashCode()
    }

    operator fun plusAssign(string: String) {
        hash = hash * 31L + string.hashCode()
    }

    fun build() = LongHash(hash)
}

inline fun LongHash(block: LongHashBuilder.() -> Unit): LongHash = LongHashBuilder().apply(block).build()

data class LongHash(val hash: Long)
