package me.devnatan.inventoryframework.intellij

// Owns the "fast preview" simulated state values for interactive mode. This is deliberately not
// part of PreviewModel itself - the model is rebuilt from source on every edit (see ViewExtractor),
// while this survives across edits until explicitly reset, so clicking around doesn't get wiped
// out by unrelated typing elsewhere in the file.
class PreviewInteractionState {

    private val values = mutableMapOf<String, Any>()
    private val history = ArrayDeque<Map<String, Any>>()

    fun reset(model: PreviewModel?) {
        values.clear()
        history.clear()
        model?.states?.forEach { values[it.id] = it.initialValue }
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    fun currentValue(stateId: String): Any? = values[stateId]

    // Reverts the most recent apply() one step at a time, like undo - not back to the initial
    // state. Returns true if there was something to undo.
    fun undo(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        values.clear()
        values.putAll(previous)
        return true
    }

    // Returns true if the action was one of the recognized/simulatable kinds.
    fun apply(action: PreviewClickAction): Boolean {
        val beforeChange = values.toMap()
        val changed = when (action) {
            is PreviewClickAction.ToggleBoolean -> {
                val current = values[action.stateId] as? Boolean
                if (current == null) {
                    false
                } else {
                    values[action.stateId] = !current
                    true
                }
            }
            is PreviewClickAction.Delta -> {
                val current = values[action.stateId] as? Int
                if (current == null) {
                    false
                } else {
                    values[action.stateId] = current + action.delta
                    true
                }
            }
            is PreviewClickAction.SetLiteral -> {
                values[action.stateId] = action.value
                true
            }
            // Navigation is handled by the caller before apply() is ever reached for this action -
            // it doesn't touch simulated state, so there's nothing to undo.
            is PreviewClickAction.OpenView -> false
            PreviewClickAction.Unsupported -> false
        }
        if (changed) history.addLast(beforeChange)
        return changed
    }

    // Overlays the current simulated state onto the model's default-resolved slots, for every
    // slot whose item is conditioned on a tracked state field.
    fun resolve(model: PreviewModel): PreviewModel {
        if (model.conditionalItems.isEmpty()) return model
        val overridden = model.slots.toMutableMap()
        for ((index, conditional) in model.conditionalItems) {
            val holds = conditional.condition.evaluate(values[conditional.condition.stateId]) ?: continue
            overridden[index] = if (holds) conditional.whenTrue else conditional.whenFalse
        }
        return model.copy(slots = overridden)
    }
}
