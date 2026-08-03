package me.devnatan.inventoryframework.intellij

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.skipParenthesizedExprDown

internal sealed class SlotTarget {
    data class Indices(val slots: List<Int>) : SlotTarget()
    data class Layout(val character: Char) : SlotTarget()
}

// Shared by ItemExtractor (withItem/renderWith/onRender) and ClickHandlerExtractor (onClick):
// both bind to the same slot(...)/layoutSlot(...)/row()/column() receiver chain, so the target
// resolution logic must stay identical between the two or their slot mappings could drift apart.
internal object SlotTargetResolver {

    // Walks up the receiver chain past any builder calls that don't define a target themselves
    // (withItem, onClick, cancelOnClick, ...) until it finds slot(...)/layoutSlot(...)/row()/column().
    fun resolve(call: UCallExpression, rows: Int, columns: Int): SlotTarget? {
        resolveDirect(call, rows, columns)?.let { return it }
        val receiverCall = asCallExpression(call.receiver) ?: return null
        return resolve(receiverCall, rows, columns)
    }

    private fun resolveDirect(call: UCallExpression, rows: Int, columns: Int): SlotTarget? {
        val args = call.valueArguments
        return when (call.methodName) {
            "slot" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { SlotTarget.Indices(listOf(it)) }
            "firstSlot" -> SlotTarget.Indices(listOf(0))
            "lastSlot" -> SlotTarget.Indices(listOf(rows * columns - 1))
            "layoutSlot" -> (args.getOrNull(0)?.evaluate() as? Char)?.let { SlotTarget.Layout(it) }
            "row" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { rowIndices(it, rows, columns) }
            "firstRow" -> rowIndices(1, rows, columns)
            "lastRow" -> rowIndices(rows, rows, columns)
            "column" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { columnIndices(it, rows, columns) }
            "firstColumn" -> columnIndices(1, rows, columns)
            "lastColumn" -> columnIndices(columns, rows, columns)
            else -> null
        }
    }

    fun rowIndices(row1Indexed: Int, rows: Int, columns: Int): SlotTarget.Indices? {
        if (row1Indexed !in 1..rows) return null
        val row0 = row1Indexed - 1
        return SlotTarget.Indices((0 until columns).map { row0 * columns + it })
    }

    fun columnIndices(column1Indexed: Int, rows: Int, columns: Int): SlotTarget.Indices? {
        if (column1Indexed !in 1..columns) return null
        val column0 = column1Indexed - 1
        return SlotTarget.Indices((0 until rows).map { it * columns + column0 })
    }

    // For row/column factory forms: `render.firstRow((pos, slot) -> ...)`. Resolves the row/column's
    // target indices and returns the lambda whose body needs to be searched for the actual binding
    // (withItem/onClick), since it operates on the lambda's own builder parameter rather than a
    // resolvable slot(...) chain.
    fun resolveFactoryLambda(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, ULambdaExpression>? {
        val args = node.valueArguments
        val (target, lambdaArg) = when (node.methodName) {
            "row" -> if (args.size == 2) {
                (args[0].evaluate() as? Int)?.let { rowIndices(it, rows, columns) }?.let { it to args[1] }
            } else null
            "firstRow" -> if (args.size == 1) rowIndices(1, rows, columns)?.let { it to args[0] } else null
            "lastRow" -> if (args.size == 1) rowIndices(rows, rows, columns)?.let { it to args[0] } else null
            "column" -> if (args.size == 2) {
                (args[0].evaluate() as? Int)?.let { columnIndices(it, rows, columns) }?.let { it to args[1] }
            } else null
            "firstColumn" -> if (args.size == 1) columnIndices(1, rows, columns)?.let { it to args[0] } else null
            "lastColumn" -> if (args.size == 1) columnIndices(columns, rows, columns)?.let { it to args[0] } else null
            else -> null
        } ?: return null

        val lambda = lambdaArg.skipParenthesizedExprDown() as? ULambdaExpression ?: return null
        return target to lambda
    }
}
