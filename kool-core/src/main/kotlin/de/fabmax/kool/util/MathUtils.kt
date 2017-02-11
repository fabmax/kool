package de.fabmax.kool.util

/**
 * @author fabmax
 */

fun toDeg(rad: Float): Float {
    return rad / Math.PI.toFloat() * 180f
}

fun toRad(rad: Float): Float {
    return rad / 180f * Math.PI.toFloat()
}

fun clamp(value: Int, min: Int = 0, max: Int = 1): Int {
    if (value < min) {
        return min
    } else if (value > max) {
        return max
    } else {
        return value
    }
}

fun clamp(value: Float, min: Float = 0f, max: Float = 1f): Float {
    if (value < min) {
        return min
    } else if (value > max) {
        return max
    } else {
        return value
    }
}

fun clamp(value: Double, min: Double = 0.0, max: Double = 1.0): Double {
    if (value < min) {
        return min
    } else if (value > max) {
        return max
    } else {
        return value
    }
}

fun isZero(value: Float): Boolean {
    // with js math library abs(Float) gives an error...
    return Math.abs(value.toDouble()) < 1e-5
}

fun isZero(value: Double): Boolean {
    return Math.abs(value) < 1e-10
}