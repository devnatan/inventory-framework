package me.devnatan.inventoryframework.intellij

data class ViewGeometry(val rows: Int, val columns: Int)

private val KNOWN_VIEW_TYPES: Map<String, ViewGeometry> = mapOf(
    "CHEST" to ViewGeometry(6, 9),
    "HOPPER" to ViewGeometry(1, 5),
    "DROPPER" to ViewGeometry(3, 3),
    "DISPENSER" to ViewGeometry(3, 3),
    "FURNACE" to ViewGeometry(2, 2),
    "BLAST_FURNACE" to ViewGeometry(2, 2),
    "CRAFTING_TABLE" to ViewGeometry(3, 3),
    "BREWING_STAND" to ViewGeometry(1, 1),
    "BEACON" to ViewGeometry(1, 1),
    "ANVIL" to ViewGeometry(1, 3),
    "SHULKER_BOX" to ViewGeometry(3, 9),
    "SMOKER" to ViewGeometry(2, 2),
    "VILLAGER_TRADING" to ViewGeometry(1, 3),
    "PLAYER" to ViewGeometry(3, 9),
)

val DEFAULT_VIEW_TYPE_NAME: String = "CHEST"
val DEFAULT_VIEW_GEOMETRY: ViewGeometry = KNOWN_VIEW_TYPES.getValue(DEFAULT_VIEW_TYPE_NAME)

fun viewGeometryFor(viewTypeFieldName: String): ViewGeometry? = KNOWN_VIEW_TYPES[viewTypeFieldName]
