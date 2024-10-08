package de.fabmax.kool.modules.ksl.lang

import de.fabmax.kool.modules.ksl.generator.KslGenerator
import de.fabmax.kool.modules.ksl.model.KslMutatedState

abstract class KslExpressionCast<T: KslType>(val value: KslExpression<*>, type: T) : KslExpression<T> {
    override val expressionType = type

    override fun collectStateDependencies(): Set<KslMutatedState> = value.collectStateDependencies()

    override fun generateExpression(generator: KslGenerator) = generator.castExpression(this)
    override fun toPseudoCode() = "cast<${expressionType.typeName}>(${value.toPseudoCode()})"
}

class KslExpressionCastScalar<S>(value: KslExpression<*>, type: S)
    : KslExpressionCast<S>(value, type), KslScalarExpression<S> where S: KslScalar, S: KslType
class KslExpressionCastVector<V, S>(value: KslExpression<*>, type: V)
    : KslExpressionCast<V>(value, type), KslVectorExpression<V, S> where V: KslVector<S>, V: KslType, S: KslScalar
