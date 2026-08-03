package me.devnatan.inventoryframework.intellij

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

// Row/column factory forms (e.g. `render.firstRow((pos, slot) -> slot.withItem(x).onClick(y))`) bind
// withItem/onClick on the lambda's own builder parameter rather than a resolvable slot(...) chain, so
// both ItemExtractor and ClickHandlerExtractor need to search the lambda body for a specific call by
// name instead of walking a receiver chain.
internal fun findCallArgumentInLambda(lambda: ULambdaExpression, methodName: String): UExpression? {
    var found: UExpression? = null
    lambda.body.accept(object : AbstractUastVisitor() {
        override fun visitCallExpression(node: UCallExpression): Boolean {
            if (found == null && node.methodName == methodName && node.valueArguments.size == 1) {
                found = node.valueArguments[0]
            }
            return found != null
        }
    })
    return found
}
