package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private const val ON_CLICK_METHOD = "onClick"
private const val AVAILABLE_SLOT_METHOD = "availableSlot"
private val ROW_COLUMN_FACTORY_METHODS = setOf("row", "firstRow", "lastRow", "column", "firstColumn", "lastColumn")
private val OPEN_VIEW_METHODS = setOf("openForPlayer", "openForEveryone")

class ClickActionExtractionResult(
    val indexed: Map<Int, PreviewClickAction>,
    val layoutBound: Map<Char, PreviewClickAction>,
    // Keyed by availableSlot(...) call site (SlotTarget.Available.anchor); ViewExtractor remaps
    // these onto real slots once AvailableSlotResolver has run.
    val availableSlotBound: Map<Int, PreviewClickAction>,
)

// Finds `.onClick(handler)` bindings, both the direct chain form ItemExtractor also resolves for
// withItem (`slot(i).onClick(...)`, possibly with a `.withItem(...)` in between) and the row/column
// factory-lambda form (`render.firstRow((pos, slot) -> slot.withItem(x).onClick(y))`), then
// pattern-matches the handler body against a small, fixed vocabulary (matchClickAction below).
// Anything outside that vocabulary is recorded as Unsupported rather than ignored, so the preview
// can say "can't simulate this" instead of silently doing nothing when clicked.
object ClickHandlerExtractor {

    fun extract(
        uFile: UFile,
        rows: Int,
        columns: Int,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): ClickActionExtractionResult {
        val indexed = mutableMapOf<Int, PreviewClickAction>()
        val layoutBound = mutableMapOf<Char, PreviewClickAction>()
        val availableSlotBound = mutableMapOf<Int, PreviewClickAction>()

        fun apply(target: SlotTarget, action: PreviewClickAction) {
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexed[it] = action }
                is SlotTarget.Layout -> layoutBound[target.character] = action
                is SlotTarget.Available -> availableSlotBound[target.anchor] = action
            }
        }

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.resolve() ?: return false
                val declaringClass = method.containingClass?.qualifiedName
                if (declaringClass == null || !declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) return false
                val methodName = node.methodName

                if (methodName == ON_CLICK_METHOD && node.valueArguments.size == 1) {
                    val receiverCall = asCallExpression(node.receiver) ?: return false
                    val target = SlotTargetResolver.resolve(receiverCall, rows, columns) ?: return false
                    val lambda = node.valueArguments[0].skipParenthesizedExprDown() as? ULambdaExpression ?: return false
                    apply(target, matchClickAction(lambda, stateIndex))
                    return false
                }

                if (methodName in ROW_COLUMN_FACTORY_METHODS) {
                    resolveFactoryClickAction(node, rows, columns, stateIndex)?.let { (target, action) ->
                        apply(target, action)
                    }
                    return false
                }

                // Only the BiConsumer factory-lambda shape of availableSlot(...) needs handling here
                // (`.availableSlot((index, builder) -> builder.onClick(...))`); the 0/1-arg chained
                // form (`.availableSlot().onClick(...)`) is already covered by the ON_CLICK_METHOD
                // branch above, whose receiver-chain walk resolves back to it via SlotTargetResolver.
                if (methodName == AVAILABLE_SLOT_METHOD) {
                    resolveFactoryClickAction(node, rows, columns, stateIndex)?.let { (target, action) ->
                        apply(target, action)
                    }
                    return false
                }

                return false
            }
        })

        return ClickActionExtractionResult(indexed, layoutBound, availableSlotBound)
    }

    private fun resolveFactoryClickAction(
        node: UCallExpression,
        rows: Int,
        columns: Int,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): Pair<SlotTarget, PreviewClickAction>? {
        val (target, lambda) = SlotTargetResolver.resolveFactoryLambda(node, rows, columns) ?: return null
        val handlerExpr = findCallArgumentInLambda(lambda, ON_CLICK_METHOD) ?: return null
        val handlerLambda = handlerExpr.skipParenthesizedExprDown() as? ULambdaExpression ?: return null
        return target to matchClickAction(handlerLambda, stateIndex)
    }

    private fun matchClickAction(
        lambda: ULambdaExpression,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): PreviewClickAction {
        val statement = singleBodyExpression(lambda.body) ?: return PreviewClickAction.Unsupported
        val call = asCallExpression(statement) ?: return PreviewClickAction.Unsupported

        matchOpenViewAction(call)?.let { return it }

        val receiver = call.receiver?.skipParenthesizedExprDown() as? UReferenceExpression
            ?: return PreviewClickAction.Unsupported
        val field = receiver.resolve() as? PsiField ?: return PreviewClickAction.Unsupported
        val declaration = stateIndex[field] ?: return PreviewClickAction.Unsupported

        return when (call.methodName) {
            "set" -> {
                val value = call.valueArguments.getOrNull(0)?.skipParenthesizedExprDown()
                    ?: return PreviewClickAction.Unsupported
                matchSetValue(value, field, declaration) ?: PreviewClickAction.Unsupported
            }
            // MutableIntState.increment/decrement(host) - the idiomatic alternative to
            // set(get(ctx) + 1, ctx), which is why both are matched independently.
            "increment" -> if (declaration.kind == PreviewStateKind.INT) {
                PreviewClickAction.Delta(declaration.id, 1)
            } else {
                PreviewClickAction.Unsupported
            }
            "decrement" -> if (declaration.kind == PreviewStateKind.INT) {
                PreviewClickAction.Delta(declaration.id, -1)
            } else {
                PreviewClickAction.Unsupported
            }
            else -> PreviewClickAction.Unsupported
        }
    }

    private fun matchSetValue(value: UExpression, field: PsiField, declaration: PreviewStateDeclaration): PreviewClickAction? {
        val literal = value as? ULiteralExpression
        if (literal != null) {
            return when (declaration.kind) {
                PreviewStateKind.BOOLEAN -> (literal.value as? Boolean)?.let { PreviewClickAction.SetLiteral(declaration.id, it) }
                PreviewStateKind.INT -> (literal.value as? Int)?.let { PreviewClickAction.SetLiteral(declaration.id, it) }
            }
        }

        if (value is UUnaryExpression && value.operator == UastPrefixOperator.LOGICAL_NOT) {
            if (declaration.kind != PreviewStateKind.BOOLEAN) return null
            return if (isGetCallOn(value.operand.skipParenthesizedExprDown(), field)) {
                PreviewClickAction.ToggleBoolean(declaration.id)
            } else {
                null
            }
        }

        if (value is UBinaryExpression) {
            if (declaration.kind != PreviewStateKind.INT) return null
            val left = value.leftOperand.skipParenthesizedExprDown()
            val right = value.rightOperand.skipParenthesizedExprDown() as? ULiteralExpression
            val delta = right?.value as? Int ?: return null
            if (!isGetCallOn(left, field)) return null
            return when (value.operator) {
                UastBinaryOperator.PLUS -> PreviewClickAction.Delta(declaration.id, delta)
                UastBinaryOperator.MINUS -> PreviewClickAction.Delta(declaration.id, -delta)
                else -> null
            }
        }

        return null
    }

    // `click.openForPlayer(OtherView.class)` / `click.openForEveryone(OtherView.class)` - unlike
    // the state-mutating shapes above, the receiver here is the click context itself rather than a
    // tracked state field, so it's matched independently before that field-resolution path runs.
    private fun matchOpenViewAction(call: UCallExpression): PreviewClickAction.OpenView? {
        if (call.methodName !in OPEN_VIEW_METHODS) return null
        val method = call.resolve() ?: return null
        val declaringClass = method.containingClass?.qualifiedName ?: return null
        if (!declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) return null
        val classLiteral = call.valueArguments.getOrNull(0)?.skipParenthesizedExprDown() as? UClassLiteralExpression
            ?: return null
        val targetClass = (classLiteral.type as? PsiClassType)?.resolve() ?: return null
        val fqn = targetClass.qualifiedName ?: return null
        return PreviewClickAction.OpenView(fqn)
    }

    private fun isGetCallOn(expr: UExpression, field: PsiField): Boolean {
        val call = asCallExpression(expr) ?: return false
        if (call.methodName != "get") return false
        val receiver = call.receiver?.skipParenthesizedExprDown() as? UReferenceExpression ?: return false
        return receiver.resolve() == field
    }
}
