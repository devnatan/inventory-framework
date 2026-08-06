package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.util.TextRange

// amount is null either because there's no ItemStack(Material, int) constructor to read from (the
// single-arg constructor, always 1) or its argument is a constant that just doesn't need a badge
// (1, 0, negative - vanilla only badges a stack of 2+). amountDynamic distinguishes that from a
// second constructor argument that genuinely couldn't be resolved to a single value (e.g. a
// reassigned/loop variable, which really does differ per read) - that case still renders, as "?",
// rather than silently looking identical to "no count at all".
data class PreviewSlot(
    val material: String?,
    val dynamic: Boolean,
    val amount: Int? = null,
    val amountDynamic: Boolean = false,
    val sourceRange: TextRange? = null,
)

enum class PreviewStateKind { BOOLEAN, INT }

data class PreviewStateDeclaration(
    val id: String,
    val kind: PreviewStateKind,
    val initialValue: Any,
    val declarationOffset: Int,
)

// A recognized shape for a withItem(...) ternary's condition. BooleanState is a direct (optionally
// negated) read of a boolean state; IntEquals is `intField.get(ctx) == N` (or `!=`), which is the
// common way an int/selection state picks between items (e.g. "is this the selected page").
sealed class PreviewCondition {
    abstract val stateId: String

    data class BooleanState(override val stateId: String, val negated: Boolean) : PreviewCondition()
    data class IntEquals(override val stateId: String, val value: Int, val negated: Boolean) : PreviewCondition()

    // Null if `current` isn't the type this condition expects (e.g. state hasn't been resolved yet).
    fun evaluate(current: Any?): Boolean? = when (this) {
        is BooleanState -> (current as? Boolean)?.let { it != negated }
        is IntEquals -> (current as? Int)?.let { (it == value) != negated }
    }
}

// A withItem(...) argument that's a ternary conditioned on a known state field, e.g.
// `withItem(enabled.get(ctx) ? onItem : offItem)` or `withItem(page.get(ctx) == 1 ? A : B)`.
// `default` is whichever branch the state's initial value picks, i.e. what's shown before any
// interaction happens.
data class ConditionalItem(
    val condition: PreviewCondition,
    val whenTrue: PreviewSlot,
    val whenFalse: PreviewSlot,
    val default: PreviewSlot,
)

// What a slot's onClick handler does to preview state, as far as the fast/offline preview can
// tell statically. Unsupported means a handler exists but its body isn't one of the recognized
// shapes - clicking it should say so rather than silently doing nothing or guessing.
sealed class PreviewClickAction {
    data class ToggleBoolean(val stateId: String) : PreviewClickAction()
    data class Delta(val stateId: String, val delta: Int) : PreviewClickAction()
    data class SetLiteral(val stateId: String, val value: Any) : PreviewClickAction()
    data class OpenView(val targetClassFqn: String) : PreviewClickAction()
    object Unsupported : PreviewClickAction()
}

data class PreviewModel(
    val viewTypeName: String,
    val rows: Int,
    val columns: Int,
    val maxSize: Int,
    val title: String?,
    val layout: List<String>?,
    val slots: Map<Int, PreviewSlot> = emptyMap(),
    val states: List<PreviewStateDeclaration> = emptyList(),
    val conditionalItems: Map<Int, ConditionalItem> = emptyMap(),
    val clickActions: Map<Int, PreviewClickAction> = emptyMap(),
)
