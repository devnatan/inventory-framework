package me.devnatan.inventoryframework.intellij

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import javax.imageio.ImageIO

private const val ICON_SIZE = 16
private const val TEXTURE_ROOT = "assets/minecraft/textures"
private const val MODEL_ROOT = "assets/minecraft/models"

// Preference order when a model declares more than one texture layer/face - layer0 is the
// standard flat-icon key ("item/generated" models), the rest are common cube-model face names;
// picking any single one is only ever an approximation of the real (sometimes multi-layered or
// tinted) icon, but it's a much closer one than the placeholder.
private val TEXTURE_KEY_PRIORITY = listOf("layer0", "all", "side", "particle", "texture", "top", "cross")

// Reads item icons directly from a Minecraft client jar already installed on this machine, on
// demand and entirely locally - the plugin itself never bundles or serves any extracted game
// asset, since Mojang's usage guidelines prohibit that for third-party tools (see
// "No real item icons" in TOOLING_SUPPORT.md). If no client jar can be found, every lookup
// returns null and callers fall back to the existing placeholder rendering.
object ItemIconProvider {

    // Sentinel distinguishing "never resolved a jar yet" from a resolved-but-null result, so a
    // client jar that's genuinely missing isn't retried (and re-scanned) on every single lookup.
    private object Unresolved

    private var resolvedForSetting: Any? = Unresolved
    private var cachedJar: ZipFile? = null
    private val cache = mutableMapOf<String, BufferedImage?>()

    @Synchronized
    fun iconFor(material: String): BufferedImage? {
        val key = material.lowercase(Locale.ROOT)
        return cache.getOrPut(key) { loadIcon(key) }
    }

    private fun loadIcon(name: String): BufferedImage? {
        val jar = clientJar() ?: return null
        val direct = readEntry(jar, "$TEXTURE_ROOT/item/$name.png") ?: readEntry(jar, "$TEXTURE_ROOT/block/$name.png")
        val raw = direct ?: modelTexturePath(jar, name)?.let { readEntry(jar, "$TEXTURE_ROOT/$it.png") }
        return raw?.let(::normalize)
    }

    // Some items have no texture file that matches their own name - a stained glass pane's icon,
    // for instance, is just its plain glass block's texture (see models/item/*_pane.json's
    // "layer0"), not a "*_pane.png" that doesn't exist. Rather than hardcoding every such case,
    // read the reference straight out of the item's own model JSON. Only resolves one model deep
    // (no parent-chain walk, no "#variable" texture substitution), so composite block-shaped
    // items whose model only inherits a texture from a parent (fences, walls, carpets, stairs...)
    // are a known remaining gap - they still fall back to the placeholder, same as before.
    private fun modelTexturePath(jar: ZipFile, name: String): String? {
        val json = readText(jar, "$MODEL_ROOT/item/$name.json") ?: return null
        val texturesBlock = Regex(""""textures"\s*:\s*\{([^}]*)}""").find(json)?.groupValues?.get(1) ?: return null
        val entries = Regex(""""(\w+)"\s*:\s*"([^"]+)"""").findAll(texturesBlock)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val value = TEXTURE_KEY_PRIORITY.firstNotNullOfOrNull { entries[it] } ?: entries.values.firstOrNull()
        return value?.removePrefix("minecraft:")?.takeUnless { it.startsWith("#") }
    }

    private fun readText(jar: ZipFile, path: String): String? {
        val entry = jar.getEntry(path) ?: return null
        return jar.getInputStream(entry).use { runCatching { it.readBytes().toString(Charsets.UTF_8) }.getOrNull() }
    }

    // Re-resolves whenever the configured override changes (e.g. the dev just saved a new path in
    // Settings > Tools > Inventory Framework), rather than caching it for the plugin's whole
    // lifetime like a plain `by lazy` would - otherwise editing the setting would need an IDE
    // restart to take effect.
    @Synchronized
    private fun clientJar(): ZipFile? {
        val configuredHome = MinecraftIconSettings.getInstance().minecraftHome.ifBlank { null }
        if (resolvedForSetting != configuredHome) {
            resolvedForSetting = configuredHome
            cache.clear()
            cachedJar = locateClientJar(configuredHome)?.let { runCatching { ZipFile(it) }.getOrNull() }
        }
        return cachedJar
    }

    private fun readEntry(jar: ZipFile, path: String): BufferedImage? {
        val entry = jar.getEntry(path) ?: return null
        return jar.getInputStream(entry).use { runCatching { ImageIO.read(it) }.getOrNull() }
    }

    // Animated textures are stored as a vertical strip of square frames (width == frame size);
    // the first frame is the top width-by-width square. Anything still bigger than one icon cell
    // afterwards (e.g. 32x32 items) is then downscaled.
    private fun normalize(image: BufferedImage): BufferedImage {
        val square = if (image.width != image.height) {
            image.getSubimage(0, 0, image.width, minOf(image.width, image.height))
        } else {
            image
        }
        if (square.width == ICON_SIZE && square.height == ICON_SIZE) return square

        val scaled = BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(square, 0, 0, ICON_SIZE, ICON_SIZE, null)
        } finally {
            g.dispose()
        }
        return scaled
    }

    // Picks the newest installed release-named version whose jar actually contains item
    // textures, falling back to whatever else is there (e.g. a modloader profile jar) sorted by
    // recency. Modloader profiles set up by the vanilla launcher often "inheritsFrom" a vanilla
    // version instead of bundling assets themselves - those are skipped by the texture check
    // rather than treated as an error, since the real vanilla version is usually also installed.
    //
    // `configuredHome` is the dev's override from Settings > Tools > Inventory Framework
    // (MinecraftIconSettings); null means it's unset and the platform default guess is used.
    private fun locateClientJar(configuredHome: String?): File? {
        val home = configuredHome?.let(::File) ?: minecraftHome() ?: return null
        val versionsDir = File(home, "versions").takeIf { it.isDirectory } ?: return null
        val candidates = versionsDir.listFiles { f -> f.isDirectory }
            ?.mapNotNull { dir -> File(dir, "${dir.name}.jar").takeIf { it.isFile } }
            ?: return null

        val releasePattern = Regex("""^\d+\.\d+(\.\d+)?$""")
        val releases = candidates.filter { releasePattern.matches(it.parentFile.name) }
            .sortedByDescending { versionSortKey(it.parentFile.name) }
        val rest = candidates.filterNot { it in releases }.sortedByDescending { it.lastModified() }

        return (releases + rest).firstOrNull(::hasItemTextures)
    }

    private fun versionSortKey(version: String): Int {
        val parts = version.split('.').map { it.toIntOrNull() ?: 0 }
        return parts.getOrElse(0) { 0 } * 1_000_000 + parts.getOrElse(1) { 0 } * 1_000 + parts.getOrElse(2) { 0 }
    }

    private fun hasItemTextures(jar: File): Boolean =
        runCatching { ZipFile(jar).use { it.getEntry("$TEXTURE_ROOT/item/apple.png") != null } }.getOrDefault(false)

    private fun minecraftHome(): File? {
        val home = System.getProperty("user.home") ?: return null
        val os = System.getProperty("os.name")?.lowercase(Locale.ROOT).orEmpty()
        val dir = when {
            os.contains("win") -> System.getenv("APPDATA")?.let { File(it, ".minecraft") } ?: File(home, "AppData/Roaming/.minecraft")
            os.contains("mac") -> File(home, "Library/Application Support/minecraft")
            else -> File(home, ".minecraft")
        }
        return dir.takeIf { it.isDirectory }
    }
}
