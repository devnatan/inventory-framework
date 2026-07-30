package me.devnatan.inventoryframework.intellij

data class PreviewSlot(val material: String?, val dynamic: Boolean)

data class PreviewModel(
    val viewTypeName: String,
    val rows: Int,
    val columns: Int,
    val title: String?,
    val layout: List<String>?,
    val slots: Map<Int, PreviewSlot> = emptyMap(),
)
