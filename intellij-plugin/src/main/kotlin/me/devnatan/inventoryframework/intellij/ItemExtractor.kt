package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.UastPostfixOperator
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val ITEM_STACK_FQN = "org.bukkit.inventory.ItemStack"
private const val MATERIAL_FQN = "org.bukkit.Material"
private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private const val AVAILABLE_SLOT_METHOD = "availableSlot"
private val ITEM_BINDING_METHODS = setOf("withItem", "renderWith", "onRender")
private val ROW_COLUMN_FACTORY_METHODS = setOf("row", "firstRow", "lastRow", "column", "firstColumn", "lastColumn")
// Java's `==`/`!=` map to IDENTITY_EQUALS/IDENTITY_NOT_EQUALS in UAST (not EQUALS/NOT_EQUALS,
// despite both rendering as "==" in asRenderString() - EQUALS is Kotlin's structural `==`).
// Support both so this also works if the framework is ever used from Kotlin view classes.
private val NOT_EQUALS_OPERATORS = setOf(UastBinaryOperator.NOT_EQUALS, UastBinaryOperator.IDENTITY_NOT_EQUALS)
private val EQUALITY_OPERATORS = setOf(UastBinaryOperator.EQUALS, UastBinaryOperator.IDENTITY_EQUALS) + NOT_EQUALS_OPERATORS

class ItemExtractionResult(
    val indexedSlots: Map<Int, PreviewSlot>,
    val layoutBindings: Map<Char, PreviewSlot>,
    val indexedConditionalItems: Map<Int, ConditionalItem>,
    val layoutConditionalItems: Map<Char, ConditionalItem>,
    // Keyed by availableSlot(...) call site (SlotTarget.Available.anchor), not a resolved slot -
    // ViewExtractor remaps these onto real slots once AvailableSlotResolver has run. Usually a
    // single-element list (the same binding repeated across every slot the anchor claims), but
    // sized to the loop's iteration count - and varying per element - when resolveAvailableSlotItems
    // recognizes the item's amount as literally the enclosing loop's own counter.
    val availableSlotBindings: Map<Int, List<PreviewSlot>>,
    val availableSlotConditionalItems: Map<Int, ConditionalItem>,
)

object ItemExtractor {

    fun extract(
        uFile: UFile,
        rows: Int,
        columns: Int,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): ItemExtractionResult {
        val indexedSlots = mutableMapOf<Int, PreviewSlot>()
        val layoutBindings = mutableMapOf<Char, PreviewSlot>()
        val indexedConditionalItems = mutableMapOf<Int, ConditionalItem>()
        val layoutConditionalItems = mutableMapOf<Char, ConditionalItem>()
        val availableSlotBindings = mutableMapOf<Int, List<PreviewSlot>>()
        val availableSlotConditionalItems = mutableMapOf<Int, ConditionalItem>()
        val reassignedVariables = collectReassignedVariables(uFile)

        fun apply(target: SlotTarget, slot: PreviewSlot?) {
            if (slot == null) return
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexedSlots[it] = slot }
                is SlotTarget.Layout -> layoutBindings[target.character] = slot
                // Reached only by the `.availableSlot().withItem(...)` chained form (via the
                // ITEM_BINDING_METHODS branch below) - the direct/factory-lambda forms bypass apply()
                // entirely for per-iteration amount support, see resolveAvailableSlotItems.
                is SlotTarget.Available -> availableSlotBindings[target.anchor] = listOf(slot)
            }
        }

        fun applyConditional(target: SlotTarget, item: ConditionalItem) {
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexedConditionalItems[it] = item }
                is SlotTarget.Layout -> layoutConditionalItems[target.character] = item
                is SlotTarget.Available -> availableSlotConditionalItems[target.anchor] = item
            }
        }

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.resolve() ?: return false
                val declaringClass = method.containingClass?.qualifiedName
                if (declaringClass == null || !declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) return false

                val methodName = node.methodName
                val range = node.sourcePsi?.textRange
                if (methodName in ITEM_BINDING_METHODS && node.valueArguments.size == 1) {
                    val receiverCall = asCallExpression(node.receiver) ?: return false
                    val target = SlotTargetResolver.resolve(receiverCall, rows, columns) ?: return false
                    if (methodName == "withItem") {
                        val conditional = resolveConditionalItem(node.valueArguments[0], stateIndex, range, reassignedVariables)
                        if (conditional != null) {
                            applyConditional(target, conditional)
                            apply(target, conditional.default)
                        } else {
                            apply(target, resolveItem(node.valueArguments[0], reassignedVariables)?.copy(sourceRange = range))
                        }
                    } else {
                        apply(target, PreviewSlot(null, dynamic = true, sourceRange = range))
                    }
                    return false
                }

                if (methodName in ROW_COLUMN_FACTORY_METHODS) {
                    resolveFactoryCall(node, rows, columns)?.let { (target, itemExpr) ->
                        apply(target, resolveItem(itemExpr, reassignedVariables)?.copy(sourceRange = range))
                    }
                    return false
                }

                // availableSlot(...) has two single-arg shapes that only differ by argument type -
                // a BiConsumer factory lambda (searched the same way as row/column factories) or a
                // direct ItemStack (the Bukkit `availableSlot(item)` sugar, handled by
                // resolveDirectItemCall like firstSlot(item)/lastSlot(item)) - so both are tried,
                // whichever matches the actual argument shape. Bypasses apply() (single-slot,
                // single-binding) since a call site inside a loop needs one binding per iteration.
                if (methodName == AVAILABLE_SLOT_METHOD) {
                    val result = resolveFactoryCall(node, rows, columns) ?: resolveDirectItemCall(node, rows, columns)
                    result?.let { (target, itemExpr) ->
                        val anchor = (target as? SlotTarget.Available)?.anchor ?: return@let
                        val items = resolveAvailableSlotItems(node, itemExpr, reassignedVariables)
                            .map { it.copy(sourceRange = range) }
                        if (items.isNotEmpty()) availableSlotBindings[anchor] = items
                    }
                    return false
                }

                resolveDirectItemCall(node, rows, columns)?.let { (target, itemExpr) ->
                    apply(target, resolveItem(itemExpr, reassignedVariables)?.copy(sourceRange = range))
                }
                return false
            }
        })

        return ItemExtractionResult(
            indexedSlots,
            layoutBindings,
            indexedConditionalItems,
            layoutConditionalItems,
            availableSlotBindings,
            availableSlotConditionalItems,
        )
    }

    // row/column/firstRow/lastRow/firstColumn/lastColumn also have a `(BiConsumer<Integer, T> factory)`
    // overload - e.g. `render.firstRow((pos, slot) -> slot.withItem(item))` - where withItem is called
    // on the lambda's builder parameter rather than chained directly onto the row/column call. Same
    // "fill the whole row/column" heuristic as SlotTargetResolver, just with the item found by
    // searching the lambda body instead of a receiver chain.
    private fun resolveFactoryCall(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, UExpression>? {
        val (target, lambda) = SlotTargetResolver.resolveFactoryLambda(node, rows, columns) ?: return null
        val itemExpr = findCallArgumentInLambda(lambda, "withItem") ?: return null
        return target to itemExpr
    }

    private fun resolveDirectItemCall(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, UExpression>? {
        val args = node.valueArguments
        return when (node.methodName) {
            "slot" -> when (args.size) {
                2 -> {
                    val index = args[0].evaluate() as? Int ?: return null
                    SlotTarget.Indices(listOf(index)) to args[1]
                }
                3 -> {
                    val row = args[0].evaluate() as? Int ?: return null
                    val column = args[1].evaluate() as? Int ?: return null
                    val index = SlotTargetResolver.slotIndex(row, column, rows, columns) ?: return null
                    SlotTarget.Indices(listOf(index)) to args[2]
                }
                else -> null
            }
            "firstSlot" -> if (args.size == 1) SlotTarget.Indices(listOf(0)) to args[0] else null
            "lastSlot" -> if (args.size == 1) SlotTarget.Indices(listOf(rows * columns - 1)) to args[0] else null
            "layoutSlot" -> if (args.size == 2) {
                val ch = args[0].evaluate() as? Char ?: return null
                SlotTarget.Layout(ch) to args[1]
            } else null
            // Only the direct-item shape (`availableSlot(item)`) belongs here; the factory-lambda
            // shape is tried first by the caller and never reaches this branch.
            "availableSlot" -> if (args.size == 1 && args[0].skipParenthesizedExprDown() !is ULambdaExpression) {
                SlotTargetResolver.anchorOf(node)?.let { SlotTarget.Available(it) to args[0] }
            } else {
                null
            }
            else -> null
        }
    }

    // availableSlot(...) inside a loop claims one slot per iteration (AvailableSlotResolver), and
    // when the item's amount argument is literally that loop's own counter, its value genuinely
    // differs per slot the same way the runtime's actual items would - e.g.
    // `for (int i = 1; i <= 5; i++) render.availableSlot(new ItemStack(Material.X, i))` really
    // does place a 1-stack, then a 2-stack, etc. In that specific recognized shape, one PreviewSlot
    // per iteration is produced (varying only the amount); ViewExtractor then zips them
    // positionally against the slots AvailableSlotResolver assigned to this call site's iterations.
    // Anything else - amount isn't a reference at all, references a different variable, the call
    // isn't directly inside a loop ForLoopAnalyzer can analyze - falls back to the single binding
    // every other availableSlot(...) shape produces.
    private fun resolveAvailableSlotItems(
        availableSlotCall: UCallExpression,
        itemExpr: UExpression,
        reassignedVariables: Set<PsiElement>,
    ): List<PreviewSlot> {
        val baseline = resolveItem(itemExpr, reassignedVariables) ?: return emptyList()
        val amountArg = amountArgumentOf(itemExpr) as? UReferenceExpression ?: return listOf(baseline)
        val iteration = availableSlotCall.getParentOfType<UForExpression>(strict = true)
            ?.let(ForLoopAnalyzer::analyze) ?: return listOf(baseline)
        if (amountArg.resolve() != iteration.variable.sourcePsi) return listOf(baseline)
        return iteration.values.map { baseline.copy(amount = it, amountDynamic = false) }
    }

    // The raw second constructor argument of an ItemStack(Material, amount[, damage]) call, if the
    // item expression resolves to one - the same argument extractAmount() reads, but as the
    // expression itself rather than an already-evaluated value, since resolveAvailableSlotItems
    // needs its identity (is it literally the loop counter?), not just its value.
    private fun amountArgumentOf(itemExpr: UExpression): UExpression? =
        findItemStackConstructorCall(itemExpr.skipParenthesizedExprDown())
            ?.valueArguments?.getOrNull(1)?.skipParenthesizedExprDown()

    // Detects `withItem(field.get(ctx) ? thenItem : elseItem)` / `withItem(!field.get(ctx) ? ... : ...)`
    // where `field` is a known boolean mutableState. Anything else (non-boolean state, a condition
    // that isn't a direct get() read, branches that aren't plain ItemStack constructors) isn't matched -
    // the slot just falls back to whatever resolveItem() makes of it (usually "dynamic").
    private fun resolveConditionalItem(
        rawExpr: UExpression?,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
        range: TextRange?,
        reassignedVariables: Set<PsiElement>,
    ): ConditionalItem? {
        val expr = rawExpr?.skipParenthesizedExprDown() as? UIfExpression ?: return null
        if (!expr.isTernary) return null
        val match = resolveCondition(expr.condition, stateIndex) ?: return null
        val thenSlot = resolveItem(expr.thenExpression, reassignedVariables)?.copy(sourceRange = range) ?: return null
        val elseSlot = resolveItem(expr.elseExpression, reassignedVariables)?.copy(sourceRange = range) ?: return null
        val holds = match.condition.evaluate(match.declaration.initialValue) ?: return null
        val default = if (holds) thenSlot else elseSlot
        return ConditionalItem(match.condition, thenSlot, elseSlot, default)
    }

    private class ConditionMatch(val condition: PreviewCondition, val declaration: PreviewStateDeclaration)

    // Recognizes `field.get(ctx)` / `!field.get(ctx)` for a boolean state, and
    // `field.get(ctx) == literal` / `!=` (either operand order) for an int state. Anything else -
    // arbitrary boolean expressions, comparisons against non-literals, non-tracked fields - isn't
    // matched, and the item stays whatever resolveItem() alone makes of it.
    private fun resolveCondition(
        rawCondition: UExpression,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): ConditionMatch? {
        val condition = rawCondition.skipParenthesizedExprDown()

        if (condition is UUnaryExpression && condition.operator == UastPrefixOperator.LOGICAL_NOT) {
            val inner = resolveCondition(condition.operand, stateIndex) ?: return null
            return ConditionMatch(negate(inner.condition), inner.declaration)
        }

        if (condition is UBinaryExpression && condition.operator in EQUALITY_OPERATORS) {
            val left = condition.leftOperand.skipParenthesizedExprDown()
            val right = condition.rightOperand.skipParenthesizedExprDown()
            val (field, literalExpr) = fieldAndLiteral(left, right, stateIndex) ?: return null
            val declaration = stateIndex.getValue(field)
            if (declaration.kind != PreviewStateKind.INT) return null
            val literalValue = (literalExpr as? ULiteralExpression)?.value as? Int ?: return null
            val negated = condition.operator in NOT_EQUALS_OPERATORS
            return ConditionMatch(PreviewCondition.IntEquals(declaration.id, literalValue, negated), declaration)
        }

        val field = resolveGetCallField(condition, stateIndex) ?: return null
        val declaration = stateIndex.getValue(field)
        if (declaration.kind != PreviewStateKind.BOOLEAN) return null
        return ConditionMatch(PreviewCondition.BooleanState(declaration.id, negated = false), declaration)
    }

    private fun negate(condition: PreviewCondition): PreviewCondition = when (condition) {
        is PreviewCondition.BooleanState -> condition.copy(negated = !condition.negated)
        is PreviewCondition.IntEquals -> condition.copy(negated = !condition.negated)
    }

    private fun fieldAndLiteral(
        left: UExpression,
        right: UExpression,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): Pair<PsiField, UExpression>? {
        resolveGetCallField(left, stateIndex)?.let { return it to right }
        resolveGetCallField(right, stateIndex)?.let { return it to left }
        return null
    }

    private fun resolveGetCallField(expr: UExpression, stateIndex: Map<PsiField, PreviewStateDeclaration>): PsiField? {
        val call = asCallExpression(expr) ?: return null
        if (call.methodName != "get") return null
        val receiver = call.receiver?.skipParenthesizedExprDown() as? UReferenceExpression ?: return null
        val field = receiver.resolve() as? PsiField ?: return null
        return field.takeIf { it in stateIndex }
    }

    private fun resolveItem(rawExpr: UExpression?, reassignedVariables: Set<PsiElement>): PreviewSlot? {
        if (rawExpr == null) return null
        val expr = rawExpr.skipParenthesizedExprDown()
        if (isNullLiteral(expr)) return null
        val constructorCall = findItemStackConstructorCall(expr)
        val material = constructorCall?.let { extractMaterialName(it) } ?: findMaterialArgument(expr)
        val amountArg = constructorCall?.valueArguments?.getOrNull(1)
        val amount = amountArg?.let { resolveConstantInt(it, reassignedVariables) }
        return PreviewSlot(
            material = material,
            dynamic = material == null,
            amount = amount,
            // A second constructor argument exists but couldn't be resolved to one value (a
            // reassigned/loop variable, which really does hold something different on each read) -
            // as opposed to no argument at all (ItemStack(Material) alone, implicitly 1).
            amountDynamic = amountArg != null && amount == null,
        )
    }

    // Reassignment makes a variable's declaration-site initializer meaningless as "the" value at
    // any later read: a loop counter's initializer is only its starting value, not what it holds
    // on a given iteration, so resolveConstantInt must never treat a reassigned variable as if its
    // initializer were constant. Scans the whole file rather than just the variable's own scope
    // since precision comes from resolving to the exact same PsiElement, not a narrower search
    // radius - consistent with the extractor's existing whole-file, not per-method, analysis.
    private fun collectReassignedVariables(uFile: UFile): Set<PsiElement> {
        val reassigned = mutableSetOf<PsiElement>()
        fun markIfReference(operand: UExpression) {
            (operand.skipParenthesizedExprDown() as? UReferenceExpression)?.resolve()?.let { reassigned += it }
        }
        uFile.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (node.operator is UastBinaryOperator.AssignOperator) markIfReference(node.leftOperand)
                return false
            }

            override fun visitPrefixExpression(node: UPrefixExpression): Boolean {
                if (node.operator == UastPrefixOperator.INC || node.operator == UastPrefixOperator.DEC) {
                    markIfReference(node.operand)
                }
                return false
            }

            override fun visitPostfixExpression(node: UPostfixExpression): Boolean {
                if (node.operator == UastPostfixOperator.INC || node.operator == UastPostfixOperator.DEC) {
                    markIfReference(node.operand)
                }
                return false
            }
        })
        return reassigned
    }

    // ItemStack(Material) has no amount argument (implicit 1); ItemStack(Material, int amount) and
    // the legacy ItemStack(Material, int amount, short damage) both carry it as the second
    // constructor argument. evaluate() alone only folds literal expressions - it doesn't
    // dereference a plain local variable reference to its initializer (same reason
    // findItemStackConstructorCall/findMaterialArgument below do their own one-level variable
    // indirection instead of relying on it) - so a variable is followed to its initializer here
    // too, but only when it's never reassigned; a reassigned variable returns null (dynamic)
    // rather than the misleading value it merely started at.
    private fun resolveConstantInt(rawExpr: UExpression, reassignedVariables: Set<PsiElement>): Int? {
        val expr = rawExpr.skipParenthesizedExprDown()
        (expr.evaluate() as? Int)?.let { return it }
        val ref = expr as? UReferenceExpression ?: return null
        val variable = ref.resolve()?.toUElementOfType<UVariable>() ?: return null
        if (variable.sourcePsi in reassignedVariables) return null
        val initializer = variable.uastInitializer ?: return null
        return resolveConstantInt(initializer, reassignedVariables)
    }

    private fun isNullLiteral(expr: UExpression): Boolean = (expr as? ULiteralExpression)?.isNull == true

    private fun findItemStackConstructorCall(rawExpr: UExpression): UCallExpression? {
        val expr = rawExpr.skipParenthesizedExprDown()
        val direct = expr as? UCallExpression
        if (direct != null) {
            if (direct.kind != UastCallKind.CONSTRUCTOR_CALL) return null
            val constructedClass = direct.classReference?.resolve() as? PsiClass ?: return null
            return if (constructedClass.qualifiedName == ITEM_STACK_FQN) direct else null
        }

        val ref = expr as? UReferenceExpression ?: return null
        val variable = ref.resolve()?.toUElementOfType<UVariable>() ?: return null
        val initializer = variable.uastInitializer ?: return null
        return findItemStackConstructorCall(initializer)
    }

    private fun extractMaterialName(constructorCall: UCallExpression): String? {
        val field = (constructorCall.valueArguments.getOrNull(0) as? UReferenceExpression)?.resolve() as? PsiField
            ?: return null
        if (field.containingClass?.qualifiedName != MATERIAL_FQN) return null
        return field.name
    }

    // Not every item comes from a bare `new ItemStack(Material.X)` - a common idiom is a helper
    // method that builds one from a Material, e.g. `ExampleUtil.displayItem(Material.STONE,
    // "Label")`. There's no way to evaluate what such a method actually returns (this is static
    // analysis, not execution), but the Material passed in is still a strong, deterministic signal
    // of what the item will be, so it's used directly as a best-effort guess. Only looks at the
    // call's own arguments (or, through a local variable, its initializer's) - not into nested
    // calls - mirroring findItemStackConstructorCall's one-level indirection.
    private fun findMaterialArgument(rawExpr: UExpression): String? {
        val call = asCallExpression(rawExpr)
        if (call != null) {
            return call.valueArguments.firstNotNullOfOrNull(::materialFieldName)
        }

        val expr = rawExpr.skipParenthesizedExprDown()
        val ref = expr as? UReferenceExpression ?: return null
        val variable = ref.resolve()?.toUElementOfType<UVariable>() ?: return null
        val initializer = variable.uastInitializer ?: return null
        return findMaterialArgument(initializer)
    }

    private fun materialFieldName(rawExpr: UExpression): String? {
        val expr = rawExpr.skipParenthesizedExprDown()
        val field = (expr as? UReferenceExpression)?.resolve() as? PsiField ?: return null
        if (field.containingClass?.qualifiedName != MATERIAL_FQN) return null
        return field.name
    }
}
