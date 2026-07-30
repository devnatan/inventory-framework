package me.devnatan.inventoryframework.intellij

data class ViewGeometry(val rows: Int, val columns: Int, val maxSize: Int)

private val KNOWN_VIEW_TYPES: Map<String, ViewGeometry> = mapOf(
    "CHEST" to ViewGeometry(6, 9, 54),
    "HOPPER" to ViewGeometry(1, 5, 5),
    "DROPPER" to ViewGeometry(3, 3, 9),
    "DISPENSER" to ViewGeometry(3, 3, 9),
    "FURNACE" to ViewGeometry(2, 2, 3),
    "BLAST_FURNACE" to ViewGeometry(2, 2, 3),
    "CRAFTING_TABLE" to ViewGeometry(3, 3, 9),
    "BREWING_STAND" to ViewGeometry(1, 1, 4),
    "BEACON" to ViewGeometry(1, 1, 1),
    "ANVIL" to ViewGeometry(1, 3, 3),
    "SHULKER_BOX" to ViewGeometry(3, 9, 27),
    "SMOKER" to ViewGeometry(2, 2, 3),
    "VILLAGER_TRADING" to ViewGeometry(1, 3, 3),
    "PLAYER" to ViewGeometry(3, 9, 54),
)

val DEFAULT_VIEW_TYPE_NAME: String = "CHEST"
val DEFAULT_VIEW_GEOMETRY: ViewGeometry = KNOWN_VIEW_TYPES.getValue(DEFAULT_VIEW_TYPE_NAME)

fun viewGeometryFor(viewTypeFieldName: String): ViewGeometry? = KNOWN_VIEW_TYPES[viewTypeFieldName]
