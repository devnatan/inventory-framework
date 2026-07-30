package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.util.TextRange

data class PreviewSlot(val material: String?, val dynamic: Boolean, val sourceRange: TextRange? = null)

data class PreviewModel(
    val viewTypeName: String,
    val rows: Int,
    val columns: Int,
    val maxSize: Int,
    val title: String?,
    val layout: List<String>?,
    val slots: Map<Int, PreviewSlot> = emptyMap(),
)
