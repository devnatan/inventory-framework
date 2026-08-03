package me.devnatan.inventoryframework.intellij

import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.skipParenthesizedExprDown

// Java's UAST bridge wraps any explicitly-qualified call (`a.b(...)`) as a
// UQualifiedReferenceExpression (receiver + selector) wherever it appears as a *value* - a method
// argument, a binary operand, a lambda return, etc. - rather than exposing the UCallExpression
// directly. Only unqualified calls (`foo()`, `new Foo()`) come through as a plain UCallExpression.
// Every place that tries to interpret an arbitrary UExpression as "is this a call" needs this.
internal fun asCallExpression(expr: UExpression?): UCallExpression? {
    return when (val e = expr?.skipParenthesizedExprDown()) {
        is UCallExpression -> e
        is UQualifiedReferenceExpression -> e.selector.skipParenthesizedExprDown() as? UCallExpression
        else -> null
    }
}

// A single-expression lambda body (`x -> foo()`) is represented as an implicit block containing an
// implicit return of the expression, same shape as a block body whose only statement is `return
// foo();`. Unwrap both so callers can treat the two forms identically.
internal fun singleBodyExpression(body: UExpression): UExpression? {
    val statement = (body as? UBlockExpression)?.expressions?.singleOrNull() ?: body
    return (statement as? UReturnExpression)?.returnExpression ?: statement
}
