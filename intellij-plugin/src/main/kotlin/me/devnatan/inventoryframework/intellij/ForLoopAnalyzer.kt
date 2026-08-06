package me.devnatan.inventoryframework.intellij

import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastPostfixOperator
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.skipParenthesizedExprDown

// The counter variable of a simple bounded counting for-loop, plus the actual sequence of values
// it takes across every statically-known iteration (e.g. [1, 2, 3, 4, 5] for
// `for (int i = 1; i <= 5; i++)`) - not just how many there are. AvailableSlotResolver only needs
// the count (how many slots a call site inside the loop claims); ItemExtractor needs the values
// themselves, for the narrower case where the loop counter is read directly as an item's amount.
internal class LoopIteration(val variable: UVariable, val values: List<Int>)

// Mirrors what a real Java for-loop actually does, but only for the canonical counting shape -
// variable on the left of the condition, stepped by a plain i++/i--/++i/--i. Anything else (the
// bound on the left, a `+= step`/`i = i + n` update, a non-literal bound, a for-each/while loop)
// returns null; callers fall back to treating the loop as unanalyzable.
internal object ForLoopAnalyzer {

    fun analyze(forExpr: UForExpression): LoopIteration? {
        val variable = (forExpr.declaration as? UDeclarationsExpression)
            ?.declarations?.singleOrNull() as? UVariable ?: return null
        val start = variable.uastInitializer?.evaluate() as? Int ?: return null

        val step = when (val update = forExpr.update?.skipParenthesizedExprDown()) {
            is UPostfixExpression -> stepOf(update.operator, update.operand, variable) ?: return null
            is UPrefixExpression -> stepOf(update.operator, update.operand, variable) ?: return null
            else -> return null
        }

        val condition = forExpr.condition?.skipParenthesizedExprDown() as? UBinaryExpression ?: return null
        if (!isReferenceTo(condition.leftOperand, variable)) return null
        val bound = condition.rightOperand.skipParenthesizedExprDown().evaluate() as? Int ?: return null

        val count = when {
            step == 1 && condition.operator == UastBinaryOperator.LESS -> bound - start
            step == 1 && condition.operator == UastBinaryOperator.LESS_OR_EQUALS -> bound - start + 1
            step == -1 && condition.operator == UastBinaryOperator.GREATER -> start - bound
            step == -1 && condition.operator == UastBinaryOperator.GREATER_OR_EQUALS -> start - bound + 1
            else -> return null
        }.coerceAtLeast(0)

        return LoopIteration(variable, List(count) { start + it * step })
    }

    private fun stepOf(operator: UastPostfixOperator, operand: UExpression, variable: UVariable): Int? {
        if (!isReferenceTo(operand, variable)) return null
        return when (operator) {
            UastPostfixOperator.INC -> 1
            UastPostfixOperator.DEC -> -1
            else -> null
        }
    }

    private fun stepOf(operator: UastPrefixOperator, operand: UExpression, variable: UVariable): Int? {
        if (!isReferenceTo(operand, variable)) return null
        return when (operator) {
            UastPrefixOperator.INC -> 1
            UastPrefixOperator.DEC -> -1
            else -> null
        }
    }

    private fun isReferenceTo(expr: UExpression, variable: UVariable): Boolean {
        val ref = expr.skipParenthesizedExprDown() as? UReferenceExpression ?: return false
        return ref.resolve() == variable.sourcePsi
    }
}
