package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiField
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.skipParenthesizedExprDown

// mutableState/mutableIntState are declared on StateAccess but resolve through whatever concrete
// class in the framework hierarchy the call is actually made against (e.g. PlatformView) - same
// reason ItemExtractor/ClickHandlerExtractor match on a package prefix rather than one exact FQN.
private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private val MUTABLE_STATE_METHODS = setOf("mutableState", "mutableIntState")

class StateExtractionResult(
    val declarations: List<PreviewStateDeclaration>,
    val index: Map<PsiField, PreviewStateDeclaration>,
)

// Only fields initialized with a boolean or int *literal* are tracked - that covers both
// mutableIntState(0) and the generic mutableState(0)/mutableState(false) (kind is inferred from
// the literal, not the method name, since `MutableState<Integer> x = mutableState(0)` is just as
// common as mutableIntState in practice). computedState and non-literal initial values are left
// out rather than guessed - these are also the only kinds ClickHandlerExtractor knows how to match.
object StateExtractor {

    fun extract(uFile: UFile): StateExtractionResult {
        val declarations = mutableListOf<PreviewStateDeclaration>()
        val index = mutableMapOf<PsiField, PreviewStateDeclaration>()

        for (uClass in uFile.classes) {
            for (field in uClass.uastDeclarations.filterIsInstance<UField>()) {
                val psiField = field.sourcePsi as? PsiField ?: continue
                val initializer = field.uastInitializer?.skipParenthesizedExprDown() as? UCallExpression ?: continue
                if (initializer.methodName !in MUTABLE_STATE_METHODS) continue
                val method = initializer.resolve() ?: continue
                val declaringClass = method.containingClass?.qualifiedName
                if (declaringClass == null || !declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) continue

                val literalValue =
                    (initializer.valueArguments.getOrNull(0)?.skipParenthesizedExprDown() as? ULiteralExpression)?.value
                val kind = when (literalValue) {
                    is Boolean -> PreviewStateKind.BOOLEAN
                    is Int -> PreviewStateKind.INT
                    else -> continue
                }

                val declaration = PreviewStateDeclaration(stateId(psiField), kind, literalValue, psiField.textRange.startOffset)
                declarations += declaration
                index[psiField] = declaration
            }
        }

        return StateExtractionResult(declarations, index)
    }

    private fun stateId(field: PsiField): String = "${field.containingClass?.qualifiedName}#${field.name}"
}
