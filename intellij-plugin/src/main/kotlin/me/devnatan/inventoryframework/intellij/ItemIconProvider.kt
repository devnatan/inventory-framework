package me.devnatan.inventoryframework.intellij

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import javax.imageio.ImageIO

private const val ICON_SIZE = 16
private const val TEXTURE_ROOT = "assets/minecraft/textures"
private const val MODEL_ROOT = "assets/minecraft/models"
private const val ITEMS_ROOT = "assets/minecraft/items"

// Every model is rendered at RENDER_SIZE * SUPERSAMPLE resolution, sampling source textures with
// hard nearest-neighbor per output pixel, then box-filtered back down to RENDER_SIZE. Nearest-
// neighbor sampling alone gives every face a jagged, aliased silhouette at any resolution that's
// only a small multiple of the source texture; supersampling first is what turns that into a
// smooth diagonal edge once downscaled. RENDER_SIZE itself is well above the panel's normal slot
// size (see SLOT_SIZE/SPRITE_SLOT_SIZE in InventoryPreviewPanel) so the extra detail survives the
// panel's own zoom levels (up to 3x) instead of visibly pixelating - the render only happens once
// per material and is cached, so the larger size costs nothing after the first paint.
private const val RENDER_SIZE = ICON_SIZE * 8
private const val SUPERSAMPLE = 4
private const val RENDER_GRID = RENDER_SIZE * SUPERSAMPLE

// Preference order for a flat (non-3D) model's texture - layer0 is the standard key for
// "item/generated" models; the rest cover models that turned out to have no usable element
// geometry, so at least one face is still shown instead of nothing.
private val FLAT_TEXTURE_KEYS = listOf("layer0", "all", "side", "particle", "texture", "top", "cross")

// Face directions visible from the fixed camera this renderer uses (see project()) - "down",
// "north" and "west" always face away from it, so parsing/rendering never needs to consider them.
private val VISIBLE_FACES = listOf("up" to 1.0, "south" to 0.8, "east" to 0.6)

private class ModelFace(val textureVariable: String, val uv: DoubleArray?)

private class ModelElement(
    val x0: Double,
    val y0: Double,
    val z0: Double,
    val x1: Double,
    val y1: Double,
    val z1: Double,
    val faces: Map<String, ModelFace>,
) {
    // Used only to order faces so nearer elements are tried first per pixel (see renderElements) -
    // the max corner (nearest to camera) of a whole element is precise enough for the simple,
    // non-interpenetrating shapes vanilla block models are built from.
    val depth: Double get() = x1 + y1 + z1
}

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
        val resolved = resolveElements(jar, name)
        val elements = resolved?.first
        if (elements != null) {
            renderElements(jar, elements, resolved.second)?.let { return it }
        }

        val direct = readEntry(jar, "$TEXTURE_ROOT/item/$name.png") ?: readEntry(jar, "$TEXTURE_ROOT/block/$name.png")
        val fromModel = resolved?.second?.let { pickTexture(jar, it, FLAT_TEXTURE_KEYS) }
        return (direct ?: fromModel)?.let(::normalize)
    }

    // Finds the real geometry behind a material's icon, however deep its model chain is, instead
    // of hardcoding a fixed set of recognized shapes: starts from the material's leaf model (which
    // may itself only be reachable through the newer assets/minecraft/items/<name>.json
    // indirection - e.g. a fence's icon is a dedicated "..._inventory" model, not "block/<name>"),
    // then walks "parent" upward, merging each level's "textures" (a child's own values win) until
    // a model with "elements" is found. Stops there rather than continuing further up, since every
    // vanilla model that defines shape also defines (or inherits from what's already been merged)
    // concrete texture values for it. The merged texture map is always returned alongside whatever
    // elements were found (possibly null, e.g. flat items like tools/food that terminate at
    // item/generated, which has neither) - callers fall back to it for a flat texture. This matters
    // even for a flat item whose own layer0 texture isn't named after the material itself (e.g. a
    // stained glass pane's flat icon reuses its stained-glass block's texture) - only this merged
    // map, not a guess based on the material's own name, can recover the right texture path there.
    private fun resolveElements(jar: ZipFile, name: String): Pair<List<ModelElement>?, Map<String, String>>? {
        var path: String? = findLeafModelPath(jar, name) ?: return null
        val textureLayers = mutableListOf<Map<String, String>>()
        var elements: List<ModelElement>? = null
        var guard = 0
        while (path != null && guard++ < 12) {
            val json = readText(jar, path) ?: break
            val obj = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: break
            textureLayers.add(parseTextures(obj))
            if (elements == null && obj.has("elements")) {
                elements = runCatching { parseElements(obj) }.getOrNull()
            }
            if (elements != null) break
            val parent = obj.takeIf { it.has("parent") }?.get("parent")?.asString
            path = parent?.let { "$MODEL_ROOT/${stripNamespace(it)}.json" }
        }

        val merged = mutableMapOf<String, String>()
        for (layer in textureLayers.asReversed()) merged.putAll(layer)
        return elements to merged
    }

    // A material's displayed model isn't always reachable by guessing "models/item/<name>.json"
    // or "models/block/<name>.json" directly - some materials (fences, walls, ...) only have a
    // dedicated icon model reachable through assets/minecraft/items/<name>.json's own "model"
    // reference. Only the simple, common "minecraft:model" reference type is followed; other
    // types (select/special/condition - used for e.g. chests or compasses to pick a model based on
    // world/item state) need real per-case handling this doesn't attempt, so those fall through to
    // the direct-path guess same as before.
    private fun findLeafModelPath(jar: ZipFile, name: String): String? {
        readText(jar, "$ITEMS_ROOT/$name.json")?.let { json ->
            val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            val modelObj = root?.takeIf { it.has("model") }?.getAsJsonObject("model")
            val type = modelObj?.takeIf { it.has("type") }?.get("type")?.asString
            if (type == "minecraft:model" && modelObj.has("model")) {
                val target = "$MODEL_ROOT/${stripNamespace(modelObj.get("model").asString)}.json"
                if (jar.getEntry(target) != null) return target
            }
        }
        val itemPath = "$MODEL_ROOT/item/$name.json"
        if (jar.getEntry(itemPath) != null) return itemPath
        val blockPath = "$MODEL_ROOT/block/$name.json"
        if (jar.getEntry(blockPath) != null) return blockPath
        return null
    }

    private fun stripNamespace(value: String): String = value.substringAfter(':')

    private fun parseTextures(obj: JsonObject): Map<String, String> {
        if (!obj.has("textures")) return emptyMap()
        return obj.getAsJsonObject("textures").entrySet().associate { it.key to it.value.asString }
    }

    private fun parseElements(obj: JsonObject): List<ModelElement> =
        obj.getAsJsonArray("elements").map { el ->
            val element = el.asJsonObject
            val from = element.getAsJsonArray("from")
            val to = element.getAsJsonArray("to")
            val faces = mutableMapOf<String, ModelFace>()
            if (element.has("faces")) {
                val facesObj = element.getAsJsonObject("faces")
                for ((faceName, _) in VISIBLE_FACES) {
                    if (!facesObj.has(faceName)) continue
                    val faceObj = facesObj.getAsJsonObject(faceName)
                    if (!faceObj.has("texture")) continue
                    val uv = faceObj.takeIf { it.has("uv") }?.getAsJsonArray("uv")
                        ?.map { it.asDouble }?.toDoubleArray()
                    faces[faceName] = ModelFace(faceObj.get("texture").asString, uv)
                }
            }
            ModelElement(
                from[0].asDouble, from[1].asDouble, from[2].asDouble,
                to[0].asDouble, to[1].asDouble, to[2].asDouble,
                faces,
            )
        }

    // Resolves a "#variable" chain (a model's texture value can itself point at another variable,
    // e.g. cube_all's "#up" resolving to "#all") to a concrete texture path, or null if it dangles
    // (undefined variable, or still unresolved after the hop limit - a cycle, or a variable that's
    // genuinely never given a concrete value in this chain).
    private fun resolveTextureVariable(textures: Map<String, String>, ref: String?, hops: Int = 0): String? {
        if (ref == null || hops > 6) return null
        if (!ref.startsWith("#")) return stripNamespace(ref)
        return resolveTextureVariable(textures, textures[ref.removePrefix("#")], hops + 1)
    }

    private fun pickTexture(jar: ZipFile, textures: Map<String, String>, keyPriority: List<String>): BufferedImage? {
        val value = keyPriority.firstNotNullOfOrNull { textures[it] } ?: textures.values.firstOrNull()
        val path = value?.let { resolveTextureVariable(textures, it) } ?: return null
        return readEntry(jar, "$TEXTURE_ROOT/$path.png")
    }

    private fun readText(jar: ZipFile, path: String): String? {
        val entry = jar.getEntry(path) ?: return null
        return jar.getInputStream(entry).use { runCatching { it.readBytes().toString(Charsets.UTF_8) }.getOrNull() }
    }

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
    // the first frame is the top width-by-width square.
    private fun cropToSquare(image: BufferedImage): BufferedImage {
        if (image.width == image.height) return image
        return image.getSubimage(0, 0, image.width, minOf(image.width, image.height))
    }

    // Scales a square texture to the fixed icon cell size used everywhere else (e.g. 32x32 items).
    private fun normalize(image: BufferedImage): BufferedImage {
        val square = cropToSquare(image)
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

    // Composites every element's visible faces into one icon, matching how Minecraft itself
    // renders a block-shaped item: not by hardcoding a handful of recognized shapes, but by
    // actually projecting whatever cuboids the model is built from (a stair is a slab plus a
    // raised quarter-block, a fence is a post plus crossbars, a torch is a single thin stick).
    // Faces are depth-sorted once (nearer elements first) and, per pixel, the first face whose
    // projected parallelogram contains that pixel wins - this correctly layers e.g. a stair's
    // raised step over the slab beneath it without needing a full z-buffer, since vanilla's simple
    // architectural shapes don't have elements that interpenetrate in more complex ways.
    private fun renderElements(jar: ZipFile, elements: List<ModelElement>, textures: Map<String, String>): BufferedImage? {
        val jobs = elements
            .flatMap { element -> VISIBLE_FACES.mapNotNull { (faceName, shade) -> renderJob(jar, textures, element, faceName, shade) } }
            .sortedByDescending { it.depth }
        if (jobs.isEmpty()) return null

        val rendered = BufferedImage(RENDER_GRID, RENDER_GRID, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until RENDER_GRID) {
            for (x in 0 until RENDER_GRID) {
                val px = x + 0.5
                val py = y + 0.5
                val argb = jobs.firstNotNullOfOrNull { it.sample(px, py) } ?: 0
                rendered.setRGB(x, y, argb)
            }
        }
        return boxDownscale(rendered, RENDER_SIZE)
    }

    private fun renderJob(jar: ZipFile, textures: Map<String, String>, element: ModelElement, faceName: String, shade: Double): RenderJob? {
        val face = element.faces[faceName] ?: return null
        val texturePath = resolveTextureVariable(textures, face.textureVariable) ?: return null
        val texture = readEntry(jar, "$TEXTURE_ROOT/$texturePath.png") ?: return null

        val corners = projectedFaceCorners(element, faceName)
        val uv = face.uv ?: autoUv(element, faceName)
        val scaleX = texture.width / 16.0
        val scaleY = texture.height / 16.0
        val u0 = uv[0] * scaleX
        val v0 = uv[1] * scaleY
        val u1 = uv[2] * scaleX
        val v1 = uv[3] * scaleY
        val w = u1 - u0
        val h = v1 - v0
        if (w == 0.0 || h == 0.0) return null

        // Maps a texture rect's own local (0,0)/(w,0)/(0,h) corners to the 3 projected destination
        // points, inverted for per-pixel sampling; (u0,v0) is added back in RenderJob.sample since
        // this only knows the rect's local origin, not its absolute position in the source texture.
        val local = AffineTransform(
            (corners[2] - corners[0]) / w, (corners[3] - corners[1]) / w,
            (corners[4] - corners[0]) / h, (corners[5] - corners[1]) / h,
            corners[0], corners[1],
        )
        val inverse = runCatching { local.createInverse() }.getOrNull() ?: return null
        return RenderJob(texture, inverse, shade, element.depth, minOf(u0, u1), minOf(v0, v1), maxOf(u0, u1), maxOf(v0, v1))
    }

    // Returns [p0x,p0y, p1x,p1y, p2x,p2y]: the destination points for the face's own (u0,v0),
    // (u1,v0) and (u0,v1) UV corners (in that order), given the fixed camera projection. Verified
    // against real model UVs (e.g. fence_inventory.json): "up" maps u/v directly to x/z, while the
    // vertical side faces ("south"/"east") map v to 16-y, since texture v grows downward while
    // world y grows upward.
    private fun projectedFaceCorners(el: ModelElement, faceName: String): DoubleArray = when (faceName) {
        "up" -> pointsToArray(project(el.x0, el.y1, el.z0), project(el.x1, el.y1, el.z0), project(el.x0, el.y1, el.z1))
        "south" -> pointsToArray(project(el.x0, el.y1, el.z1), project(el.x1, el.y1, el.z1), project(el.x0, el.y0, el.z1))
        "east" -> pointsToArray(project(el.x1, el.y1, el.z0), project(el.x1, el.y1, el.z1), project(el.x1, el.y0, el.z0))
        else -> error("unsupported face $faceName")
    }

    private fun pointsToArray(a: Point2D, b: Point2D, c: Point2D) = doubleArrayOf(a.x, a.y, b.x, b.y, c.x, c.y)

    // Used only when a face has no explicit "uv" (implicit UV = matches the element's own position
    // and size on the relevant two axes), which holds for every vanilla model observed so far.
    private fun autoUv(el: ModelElement, faceName: String): DoubleArray = when (faceName) {
        "up" -> doubleArrayOf(el.x0, el.z0, el.x1, el.z1)
        "south" -> doubleArrayOf(el.x0, 16 - el.y1, el.x1, 16 - el.y0)
        "east" -> doubleArrayOf(el.z0, 16 - el.y1, el.z1, 16 - el.y0)
        else -> error("unsupported face $faceName")
    }

    // Fixed dimetric camera projection, derived from and matching the hand-tuned, user-verified
    // full-cube/slab geometry this replaces: independent of y for screen-x, and a linear blend of
    // x/y/z for screen-y. Coordinates are 0-16 Minecraft model units; output is in render-grid
    // pixels.
    private fun project(x: Double, y: Double, z: Double): Point2D {
        val nx = x / 16.0
        val ny = y / 16.0
        val nz = z / 16.0
        val sx = 0.5 * (nx - nz + 1)
        val sy = 0.25 * (nx + nz) - 0.5 * ny + 0.5
        return Point2D.Double(sx * RENDER_GRID, sy * RENDER_GRID)
    }

    private class RenderJob(
        val texture: BufferedImage,
        val inverse: AffineTransform,
        val shade: Double,
        val depth: Double,
        val u0: Double,
        val v0: Double,
        val u1: Double,
        val v1: Double,
    ) {
        // Null means the render-grid point (px, py) falls outside this face's own rect once
        // mapped back to local texture-rect coordinates - the caller tries the next face, or
        // leaves the pixel transparent if none hit.
        fun sample(px: Double, py: Double): Int? {
            val local = inverse.transform(Point2D.Double(px, py), null)
            if (local.x < 0 || local.y < 0 || local.x >= (u1 - u0) || local.y >= (v1 - v0)) return null
            val u = Math.floor(local.x + u0).toInt()
            val v = Math.floor(local.y + v0).toInt()
            if (u < 0 || v < 0 || u >= texture.width || v >= texture.height) return null
            val argb = texture.getRGB(u, v)
            val a = (argb shr 24) and 0xFF
            if (a == 0) return null
            val r = (((argb shr 16) and 0xFF) * shade).toInt().coerceIn(0, 255)
            val g = (((argb shr 8) and 0xFF) * shade).toInt().coerceIn(0, 255)
            val b = ((argb and 0xFF) * shade).toInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    // Averages every (factor x factor) block of the supersampled render into one output pixel,
    // alpha-weighted so a face's real edge color doesn't get diluted by the transparent pixels
    // just outside it. This is what turns the hard, jagged per-pixel face boundaries above into a
    // smooth antialiased silhouette.
    private fun boxDownscale(source: BufferedImage, targetSize: Int): BufferedImage {
        val factor = source.width / targetSize
        val out = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                var alphaSum = 0L
                var r = 0L
                var g = 0L
                var b = 0L
                for (sy in 0 until factor) {
                    for (sx in 0 until factor) {
                        val argb = source.getRGB(x * factor + sx, y * factor + sy)
                        val a = (argb shr 24) and 0xFF
                        alphaSum += a
                        r += ((argb shr 16) and 0xFF) * a
                        g += ((argb shr 8) and 0xFF) * a
                        b += (argb and 0xFF) * a
                    }
                }
                val sampleCount = (factor * factor).toLong()
                val outAlpha = (alphaSum / sampleCount).toInt()
                val outR = if (alphaSum == 0L) 0 else (r / alphaSum).toInt()
                val outG = if (alphaSum == 0L) 0 else (g / alphaSum).toInt()
                val outB = if (alphaSum == 0L) 0 else (b / alphaSum).toInt()
                out.setRGB(x, y, (outAlpha shl 24) or (outR shl 16) or (outG shl 8) or outB)
            }
        }
        return out
    }

    // Picks the newest installed release-named version whose jar actually contains item
    // textures, falling back to whatever else is there (e.g. a modloader profile jar) sorted by
    // recency. Modloader profiles set up by the vanilla launcher often "inheritsFrom" a vanilla
    // version instead of bundling assets themselves - those are skipped by the texture check
    // rather than treated as an error, since the real vanilla version is usually also installed.
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
