package de.fabmax.kool.math

import de.fabmax.kool.util.Float32Buffer
import de.fabmax.kool.util.Uint32Buffer
import kotlin.math.sqrt

fun Vec3f.toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec3f.toMutableVec3d(result: MutableVec3d = MutableVec3d()) = result.set(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec3f.toVec3i() = Vec3i(x.toInt(), y.toInt(), z.toInt())
fun Vec3f.toMutableVec3i(result: MutableVec3i = MutableVec3i()) = result.set(x.toInt(), y.toInt(), z.toInt())
fun MutableVec3f.set(that: Vec3d) = set(that.x.toFloat(), that.y.toFloat(), that.z.toFloat())
fun MutableVec3f.set(that: Vec3i) = set(that.x.toFloat(), that.y.toFloat(), that.z.toFloat())

fun Vec3d.toVec3f() = Vec3f(x.toFloat(), y.toFloat(), z.toFloat())
fun Vec3d.toMutableVec3f(result: MutableVec3f = MutableVec3f()) = result.set(x.toFloat(), y.toFloat(), z.toFloat())
fun Vec3d.toVec3i() = Vec3i(x.toInt(), y.toInt(), z.toInt())
fun Vec3d.toMutableVec3i(result: MutableVec3i = MutableVec3i()) = result.set(x.toInt(), y.toInt(), z.toInt())
fun MutableVec3d.set(that: Vec3f) = set(that.x.toDouble(), that.y.toDouble(), that.z.toDouble())
fun MutableVec3d.set(that: Vec3i) = set(that.x.toDouble(), that.y.toDouble(), that.z.toDouble())

fun Vec3i.toVec3f() = Vec3f(x.toFloat(), y.toFloat(), z.toFloat())
fun Vec3i.toMutableVec3f(result: MutableVec3f = MutableVec3f()) = result.set(x.toFloat(), y.toFloat(), z.toFloat())
fun Vec3i.toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec3i.toMutableVec3d(result: MutableVec3d = MutableVec3d()) = result.set(x.toDouble(), y.toDouble(), z.toDouble())
fun MutableVec3i.set(that: Vec3f) = set(that.x.toInt(), that.y.toInt(), that.z.toInt())
fun MutableVec3i.set(that: Vec3d) = set(that.x.toInt(), that.y.toInt(), that.z.toInt())

// <template> Changes made within the template section will also affect the other type variants of this class

open class Vec3f(x: Float, y: Float, z: Float) {

    protected val fields = floatArrayOf(x, y, z)

    open val x get() = fields[0]
    open val y get() = fields[1]
    open val z get() = fields[2]

    constructor(f: Float) : this(f, f, f)
    constructor(v: Vec3f) : this(v.x, v.y, v.z)

    /**
     * Component-wise addition with the given [Vec3f]. Returns the result as a new [Vec3f]. Consider using [add] with
     * a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun plus(that: Vec3f) = Vec3f(x + that.x, y + that.y, z + that.z)

    /**
     * Component-wise subtraction with the given [Vec3f]. Returns the result as a new [Vec3f]. Consider using [subtract]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun minus(that: Vec3f) = Vec3f(x - that.x, y - that.y, z - that.z)

    /**
     * Component-wise multiplication with the given [Vec3f]. Returns the result as a new [Vec3f]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun times(that: Vec3f) = Vec3f(x * that.x, y * that.y, z * that.z)

    /**
     * Component-wise division with the given [Vec3f]. Returns the result as a new [Vec3f]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun div(that: Vec3f) = Vec3f(x / that.x, y / that.y, z / that.z)

    /**
     * Component-wise addition with the given [Vec3f]. Returns the result as an (optionally provided) [MutableVec3f].
     */
    fun add(other: Vec3f, result: MutableVec3f = MutableVec3f()): MutableVec3f = result.set(this).add(other)

    /**
     * Component-wise subtraction with the given [Vec3f]. Returns the result as an (optionally provided) [MutableVec3f].
     */
    fun subtract(other: Vec3f, result: MutableVec3f = MutableVec3f()): MutableVec3f = result.set(this).subtract(other)

    /**
     * Component-wise multiplication with the given [Vec3f]. Returns the result as an (optionally provided) [MutableVec3f].
     */
    fun mul(other: Vec3f, result: MutableVec3f = MutableVec3f()): MutableVec3f = result.set(this).mul(other)

    /**
     * Scales this vector by the factor and returns the result as an (optionally provided) [MutableVec3f].
     */
    fun scale(factor: Float, result: MutableVec3f = MutableVec3f()): MutableVec3f = result.set(this).scale(factor)

    override fun toString(): String = "($x, $y, $z)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability consider using
     * [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3f) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    /**
     * Appends the components of this [Vec3f] to the given [Float32Buffer].
     */
    fun putTo(target: Float32Buffer) {
        target.put(fields)
    }

    // <noInt> The following section will not be included in the integer variant of this class

    /**
     * Computes the cross-product of this and the given vector and returns the result as an (optionally provided)
     * [MutableVec3f].
     */
    fun cross(that: Vec3f, result: MutableVec3f): MutableVec3f {
        result.x = y * that.z - z * that.y
        result.y = z * that.x - x * that.z
        result.z = x * that.y - y * that.x
        return result
    }

    /**
     * Computes the Euclidean distance between this and the given vector.
     */
    fun distance(that: Vec3f): Float = sqrt(sqrDistance(that))

    /**
     * Computes the squared Euclidean distance between this and the given vector.
     */
    fun sqrDistance(that: Vec3f): Float {
        val dx = x - that.x
        val dy = y - that.y
        val dz = z - that.z
        return dx*dx + dy*dy + dz*dz
    }

    /**
     * Computes the dot-product of this and the given vector.
     */
    fun dot(that: Vec3f): Float = x * that.x + y * that.y + z * that.z

    /**
     * Computes the length / magnitude of this vector.
     */
    fun length(): Float = sqrt(sqrLength())

    /**
     * Computes the squared length / magnitude of this vector.
     */
    fun sqrLength(): Float = x*x + y*y + z*z

    /**
     * Linearly interpolates the values of this and another vector and returns the result as an (optionally provided)
     * [MutableVec3f]: result = that * weight + this * (1 - weight).
     */
    fun mix(that: Vec3f, weight: Float, result: MutableVec3f): MutableVec3f {
        result.x = that.x * weight + x * (1f - weight)
        result.y = that.y * weight + y * (1f - weight)
        result.z = that.z * weight + z * (1f - weight)
        return result
    }

    /**
     * Norms the length of this vector and returns the result as an (optionally provided) [MutableVec3f].
     */
    fun norm(result: MutableVec3f): MutableVec3f = result.set(this).norm()

    /**
     * Returns a unit vector orthogonal to this vector.
     */
    fun ortho(result: MutableVec3f = MutableVec3f()): MutableVec3f {
        val ax = when {
            this.dot(X_AXIS) < 0.5f -> X_AXIS
            this.dot(Y_AXIS) < 0.5f -> Y_AXIS
            else -> Z_AXIS
        }
        return ax.cross(this, result).norm()
    }

    /**
     * Rotates this vector by the given [AngleF] around the given axis and returns the result as an (optionally
     * provided) [MutableVec3f].
     */
    fun rotate(angle: AngleF, axis: Vec3f, result: MutableVec3f = MutableVec3f()): MutableVec3f {
        return result.set(this).rotate(angle, axis)
    }

    /**
     * Checks vector components for equality using [de.fabmax.kool.math.isFuzzyEqual], that is all components must
     * have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec3f, eps: Float = FUZZY_EQ_F): Boolean {
        return isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps) && isFuzzyEqual(z, other.z, eps)
    }

    // </noInt>

    companion object {
        val ZERO = Vec3f(0f)
        val ONES = Vec3f(1f)
        val X_AXIS = Vec3f(1f, 0f, 0f)
        val Y_AXIS = Vec3f(0f, 1f, 0f)
        val Z_AXIS = Vec3f(0f, 0f, 1f)
        val NEG_X_AXIS = Vec3f(-1f, 0f, 0f)
        val NEG_Y_AXIS = Vec3f(0f, -1f, 0f)
        val NEG_Z_AXIS = Vec3f(0f, 0f, -1f)
    }
}

open class MutableVec3f(x: Float, y: Float, z: Float) : Vec3f(x, y, z) {

    override var x
        get() = fields[0]
        set(value) { fields[0] = value }
    override var y
        get() = fields[1]
        set(value) { fields[1] = value }
    override var z
        get() = fields[2]
        set(value) { fields[2] = value }

    val array: FloatArray
        get() = fields

    constructor() : this(0f, 0f, 0f)
    constructor(f: Float) : this(f, f, f)
    constructor(v: Vec3f) : this(v.x, v.y, v.z)

    fun set(x: Float, y: Float, z: Float): MutableVec3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vec3f): MutableVec3f {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    /**
     * Inplace operation: Adds the given [Vec3f] component-wise to this vector.
     */
    operator fun plusAssign(other: Vec3f) { add(other) }

    /**
     * Inplace operation: Subtracts the given [Vec3f] component-wise from this vector.
     */
    operator fun minusAssign(other: Vec3f) { subtract(other) }

    /**
     * Inplace operation: Multiplies the given [Vec3f] component-wise with this vector.
     */
    operator fun timesAssign(factor: Float) { scale(factor) }

    /**
     * Inplace operation: Divides the given [Vec3f] component-wise with this vector.
     */
    operator fun divAssign(div: Float) { scale(1f / div) }

    /**
     * Inplace operation: Adds the given [Vec3f] component-wise to this vector.
     */
    fun add(other: Vec3f): MutableVec3f {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    /**
     * Inplace operation: Subtracts the given [Vec3f] component-wise from this vector.
     */
    fun subtract(other: Vec3f): MutableVec3f {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    /**
     * Inplace operation: Multiplies the given [Vec3f] component-wise with this vector.
     */
    fun mul(other: Vec3f): MutableVec3f {
        x *= other.x
        y *= other.y
        z *= other.z
        return this
    }

    /**
     * Inplace operation: Scales this vector by the given factor.
     */
    fun scale(factor: Float): MutableVec3f {
        x *= factor
        y *= factor
        z *= factor
        return this
    }

    // <noInt> The following section will not be included in the integer variant of this class

    /**
     * Inplace operation: Scales this vector to unit length.
     */
    fun norm(): MutableVec3f {
        val l = length()
        return if (l != 0f) {
            scale(1f / l)
        } else {
            set(ZERO)
        }
    }

    /**
     * Inplace operation: Rotates this vector by the given [AngleF] around the given axis.
     */
    fun rotate(angle: AngleF, axis: Vec3f): MutableVec3f {
        val c = angle.cos
        val c1 = 1f - c
        val s = angle.sin

        val axX = axis.x
        val axY = axis.y
        val axZ = axis.z

        val rx = x * (axX * axX * c1 + c) + y * (axX * axY * c1 - axZ * s) + z * (axX * axZ * c1 + axY * s)
        val ry = x * (axY * axX * c1 + axZ * s) + y * (axY * axY * c1 + c) + z * (axY * axZ * c1 - axX * s)
        val rz = x * (axX * axZ * c1 - axY * s) + y * (axY * axZ * c1 + axX * s) + z * (axZ * axZ * c1 + c)
        x = rx
        y = ry
        z = rz
        return this
    }

    // </noInt>
}

// </template> End of template section, DO NOT EDIT BELOW THIS!


open class Vec3d(x: Double, y: Double, z: Double) {

    protected val fields = doubleArrayOf(x, y, z)

    open val x get() = fields[0]
    open val y get() = fields[1]
    open val z get() = fields[2]

    constructor(f: Double) : this(f, f, f)
    constructor(v: Vec3d) : this(v.x, v.y, v.z)

    /**
     * Component-wise addition with the given [Vec3d]. Returns the result as a new [Vec3d]. Consider using [add] with
     * a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun plus(that: Vec3d) = Vec3d(x + that.x, y + that.y, z + that.z)

    /**
     * Component-wise subtraction with the given [Vec3d]. Returns the result as a new [Vec3d]. Consider using [subtract]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun minus(that: Vec3d) = Vec3d(x - that.x, y - that.y, z - that.z)

    /**
     * Component-wise multiplication with the given [Vec3d]. Returns the result as a new [Vec3d]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun times(that: Vec3d) = Vec3d(x * that.x, y * that.y, z * that.z)

    /**
     * Component-wise division with the given [Vec3d]. Returns the result as a new [Vec3d]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun div(that: Vec3d) = Vec3d(x / that.x, y / that.y, z / that.z)

    /**
     * Component-wise addition with the given [Vec3d]. Returns the result as an (optionally provided) [MutableVec3d].
     */
    fun add(other: Vec3d, result: MutableVec3d = MutableVec3d()): MutableVec3d = result.set(this).add(other)

    /**
     * Component-wise subtraction with the given [Vec3d]. Returns the result as an (optionally provided) [MutableVec3d].
     */
    fun subtract(other: Vec3d, result: MutableVec3d = MutableVec3d()): MutableVec3d = result.set(this).subtract(other)

    /**
     * Component-wise multiplication with the given [Vec3d]. Returns the result as an (optionally provided) [MutableVec3d].
     */
    fun mul(other: Vec3d, result: MutableVec3d = MutableVec3d()): MutableVec3d = result.set(this).mul(other)

    /**
     * Scales this vector by the factor and returns the result as an (optionally provided) [MutableVec3d].
     */
    fun scale(factor: Double, result: MutableVec3d = MutableVec3d()): MutableVec3d = result.set(this).scale(factor)

    override fun toString(): String = "($x, $y, $z)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability consider using
     * [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3d) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    /**
     * Appends the components of this [Vec3d] to the given [Float32Buffer].
     */
    fun putTo(target: Float32Buffer) {
        target.put(fields)
    }

    /**
     * Computes the cross-product of this and the given vector and returns the result as an (optionally provided)
     * [MutableVec3d].
     */
    fun cross(that: Vec3d, result: MutableVec3d): MutableVec3d {
        result.x = y * that.z - z * that.y
        result.y = z * that.x - x * that.z
        result.z = x * that.y - y * that.x
        return result
    }

    /**
     * Computes the Euclidean distance between this and the given vector.
     */
    fun distance(that: Vec3d): Double = sqrt(sqrDistance(that))

    /**
     * Computes the squared Euclidean distance between this and the given vector.
     */
    fun sqrDistance(that: Vec3d): Double {
        val dx = x - that.x
        val dy = y - that.y
        val dz = z - that.z
        return dx*dx + dy*dy + dz*dz
    }

    /**
     * Computes the dot-product of this and the given vector.
     */
    fun dot(that: Vec3d): Double = x * that.x + y * that.y + z * that.z

    /**
     * Computes the length / magnitude of this vector.
     */
    fun length(): Double = sqrt(sqrLength())

    /**
     * Computes the squared length / magnitude of this vector.
     */
    fun sqrLength(): Double = x*x + y*y + z*z

    /**
     * Linearly interpolates the values of this and another vector and returns the result as an (optionally provided)
     * [MutableVec3d]: result = that * weight + this * (1 - weight).
     */
    fun mix(that: Vec3d, weight: Double, result: MutableVec3d): MutableVec3d {
        result.x = that.x * weight + x * (1.0 - weight)
        result.y = that.y * weight + y * (1.0 - weight)
        result.z = that.z * weight + z * (1.0 - weight)
        return result
    }

    /**
     * Norms the length of this vector and returns the result as an (optionally provided) [MutableVec3d].
     */
    fun norm(result: MutableVec3d): MutableVec3d = result.set(this).norm()

    /**
     * Returns a unit vector orthogonal to this vector.
     */
    fun ortho(result: MutableVec3d = MutableVec3d()): MutableVec3d {
        val ax = when {
            this.dot(X_AXIS) < 0.5 -> X_AXIS
            this.dot(Y_AXIS) < 0.5 -> Y_AXIS
            else -> Z_AXIS
        }
        return ax.cross(this, result).norm()
    }

    /**
     * Rotates this vector by the given [AngleD] around the given axis and returns the result as an (optionally
     * provided) [MutableVec3d].
     */
    fun rotate(angle: AngleD, axis: Vec3d, result: MutableVec3d = MutableVec3d()): MutableVec3d {
        return result.set(this).rotate(angle, axis)
    }

    /**
     * Checks vector components for equality using [de.fabmax.kool.math.isFuzzyEqual], that is all components must
     * have a difference less or equal [eps].
     */
    fun isFuzzyEqual(other: Vec3d, eps: Double = FUZZY_EQ_D): Boolean {
        return isFuzzyEqual(x, other.x, eps) && isFuzzyEqual(y, other.y, eps) && isFuzzyEqual(z, other.z, eps)
    }

    companion object {
        val ZERO = Vec3d(0.0)
        val ONES = Vec3d(1.0)
        val X_AXIS = Vec3d(1.0, 0.0, 0.0)
        val Y_AXIS = Vec3d(0.0, 1.0, 0.0)
        val Z_AXIS = Vec3d(0.0, 0.0, 1.0)
        val NEG_X_AXIS = Vec3d(-1.0, 0.0, 0.0)
        val NEG_Y_AXIS = Vec3d(0.0, -1.0, 0.0)
        val NEG_Z_AXIS = Vec3d(0.0, 0.0, -1.0)
    }
}

open class MutableVec3d(x: Double, y: Double, z: Double) : Vec3d(x, y, z) {

    override var x
        get() = fields[0]
        set(value) { fields[0] = value }
    override var y
        get() = fields[1]
        set(value) { fields[1] = value }
    override var z
        get() = fields[2]
        set(value) { fields[2] = value }

    val array: DoubleArray
        get() = fields

    constructor() : this(0.0, 0.0, 0.0)
    constructor(f: Double) : this(f, f, f)
    constructor(v: Vec3d) : this(v.x, v.y, v.z)

    fun set(x: Double, y: Double, z: Double): MutableVec3d {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vec3d): MutableVec3d {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    /**
     * Inplace operation: Adds the given [Vec3d] component-wise to this vector.
     */
    operator fun plusAssign(other: Vec3d) { add(other) }

    /**
     * Inplace operation: Subtracts the given [Vec3d] component-wise from this vector.
     */
    operator fun minusAssign(other: Vec3d) { subtract(other) }

    /**
     * Inplace operation: Multiplies the given [Vec3d] component-wise with this vector.
     */
    operator fun timesAssign(factor: Double) { scale(factor) }

    /**
     * Inplace operation: Divides the given [Vec3d] component-wise with this vector.
     */
    operator fun divAssign(div: Double) { scale(1.0 / div) }

    /**
     * Inplace operation: Adds the given [Vec3d] component-wise to this vector.
     */
    fun add(other: Vec3d): MutableVec3d {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    /**
     * Inplace operation: Subtracts the given [Vec3d] component-wise from this vector.
     */
    fun subtract(other: Vec3d): MutableVec3d {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    /**
     * Inplace operation: Multiplies the given [Vec3d] component-wise with this vector.
     */
    fun mul(other: Vec3d): MutableVec3d {
        x *= other.x
        y *= other.y
        z *= other.z
        return this
    }

    /**
     * Inplace operation: Scales this vector by the given factor.
     */
    fun scale(factor: Double): MutableVec3d {
        x *= factor
        y *= factor
        z *= factor
        return this
    }

    /**
     * Inplace operation: Scales this vector to unit length.
     */
    fun norm(): MutableVec3d {
        val l = length()
        return if (l != 0.0) {
            scale(1.0 / l)
        } else {
            set(ZERO)
        }
    }

    /**
     * Inplace operation: Rotates this vector by the given [AngleD] around the given axis.
     */
    fun rotate(angle: AngleD, axis: Vec3d): MutableVec3d {
        val c = angle.cos
        val c1 = 1.0 - c
        val s = angle.sin

        val axX = axis.x
        val axY = axis.y
        val axZ = axis.z

        val rx = x * (axX * axX * c1 + c) + y * (axX * axY * c1 - axZ * s) + z * (axX * axZ * c1 + axY * s)
        val ry = x * (axY * axX * c1 + axZ * s) + y * (axY * axY * c1 + c) + z * (axY * axZ * c1 - axX * s)
        val rz = x * (axX * axZ * c1 - axY * s) + y * (axY * axZ * c1 + axX * s) + z * (axZ * axZ * c1 + c)
        x = rx
        y = ry
        z = rz
        return this
    }

}


open class Vec3i(x: Int, y: Int, z: Int) {

    protected val fields = intArrayOf(x, y, z)

    open val x get() = fields[0]
    open val y get() = fields[1]
    open val z get() = fields[2]

    constructor(f: Int) : this(f, f, f)
    constructor(v: Vec3i) : this(v.x, v.y, v.z)

    /**
     * Component-wise addition with the given [Vec3i]. Returns the result as a new [Vec3i]. Consider using [add] with
     * a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun plus(that: Vec3i) = Vec3i(x + that.x, y + that.y, z + that.z)

    /**
     * Component-wise subtraction with the given [Vec3i]. Returns the result as a new [Vec3i]. Consider using [subtract]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun minus(that: Vec3i) = Vec3i(x - that.x, y - that.y, z - that.z)

    /**
     * Component-wise multiplication with the given [Vec3i]. Returns the result as a new [Vec3i]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun times(that: Vec3i) = Vec3i(x * that.x, y * that.y, z * that.z)

    /**
     * Component-wise division with the given [Vec3i]. Returns the result as a new [Vec3i]. Consider using [mul]
     * with a pre-allocated result vector in performance-critical situations, to avoid unnecessary object allocations.
     */
    operator fun div(that: Vec3i) = Vec3i(x / that.x, y / that.y, z / that.z)

    /**
     * Component-wise addition with the given [Vec3i]. Returns the result as an (optionally provided) [MutableVec3i].
     */
    fun add(other: Vec3i, result: MutableVec3i = MutableVec3i()): MutableVec3i = result.set(this).add(other)

    /**
     * Component-wise subtraction with the given [Vec3i]. Returns the result as an (optionally provided) [MutableVec3i].
     */
    fun subtract(other: Vec3i, result: MutableVec3i = MutableVec3i()): MutableVec3i = result.set(this).subtract(other)

    /**
     * Component-wise multiplication with the given [Vec3i]. Returns the result as an (optionally provided) [MutableVec3i].
     */
    fun mul(other: Vec3i, result: MutableVec3i = MutableVec3i()): MutableVec3i = result.set(this).mul(other)

    /**
     * Scales this vector by the factor and returns the result as an (optionally provided) [MutableVec3i].
     */
    fun scale(factor: Int, result: MutableVec3i = MutableVec3i()): MutableVec3i = result.set(this).scale(factor)

    override fun toString(): String = "($x, $y, $z)"

    /**
     * Checks vector components for equality (using '==' operator). For better numeric stability consider using
     * [isFuzzyEqual].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3i) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    /**
     * Appends the components of this [Vec3i] to the given [Uint32Buffer].
     */
    fun putTo(target: Uint32Buffer) {
        target.put(fields)
    }

    companion object {
        val ZERO = Vec3i(0)
        val ONES = Vec3i(1)
        val X_AXIS = Vec3i(1, 0, 0)
        val Y_AXIS = Vec3i(0, 1, 0)
        val Z_AXIS = Vec3i(0, 0, 1)
        val NEG_X_AXIS = Vec3i(-1, 0, 0)
        val NEG_Y_AXIS = Vec3i(0, -1, 0)
        val NEG_Z_AXIS = Vec3i(0, 0, -1)
    }
}

open class MutableVec3i(x: Int, y: Int, z: Int) : Vec3i(x, y, z) {

    override var x
        get() = fields[0]
        set(value) { fields[0] = value }
    override var y
        get() = fields[1]
        set(value) { fields[1] = value }
    override var z
        get() = fields[2]
        set(value) { fields[2] = value }

    val array: IntArray
        get() = fields

    constructor() : this(0, 0, 0)
    constructor(f: Int) : this(f, f, f)
    constructor(v: Vec3i) : this(v.x, v.y, v.z)

    fun set(x: Int, y: Int, z: Int): MutableVec3i {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(other: Vec3i): MutableVec3i {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    /**
     * Inplace operation: Adds the given [Vec3i] component-wise to this vector.
     */
    operator fun plusAssign(other: Vec3i) { add(other) }

    /**
     * Inplace operation: Subtracts the given [Vec3i] component-wise from this vector.
     */
    operator fun minusAssign(other: Vec3i) { subtract(other) }

    /**
     * Inplace operation: Multiplies the given [Vec3i] component-wise with this vector.
     */
    operator fun timesAssign(factor: Int) { scale(factor) }

    /**
     * Inplace operation: Divides the given [Vec3i] component-wise with this vector.
     */
    operator fun divAssign(div: Int) { scale(1 / div) }

    /**
     * Inplace operation: Adds the given [Vec3i] component-wise to this vector.
     */
    fun add(other: Vec3i): MutableVec3i {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    /**
     * Inplace operation: Subtracts the given [Vec3i] component-wise from this vector.
     */
    fun subtract(other: Vec3i): MutableVec3i {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    /**
     * Inplace operation: Multiplies the given [Vec3i] component-wise with this vector.
     */
    fun mul(other: Vec3i): MutableVec3i {
        x *= other.x
        y *= other.y
        z *= other.z
        return this
    }

    /**
     * Inplace operation: Scales this vector by the given factor.
     */
    fun scale(factor: Int): MutableVec3i {
        x *= factor
        y *= factor
        z *= factor
        return this
    }
}
