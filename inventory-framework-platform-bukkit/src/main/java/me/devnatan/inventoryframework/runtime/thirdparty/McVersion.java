package me.devnatan.inventoryframework.runtime.thirdparty;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;

public class McVersion implements Comparable<McVersion> {

    private static final Pattern LEADING_VERSION =
            Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private static final McVersion CURRENT_VERSION;

    static {
        CURRENT_VERSION = parse(Bukkit.getBukkitVersion());
    }

    /**
     * Reads only the leading run of dot-separated numeric segments (major[.minor[.patch]]),
     * so a build/commit suffix appended by a non-standard server fork (e.g. "26.2.build.17406-6bc38be")
     * is ignored instead of throwing a {@link NumberFormatException} out of a static initializer.
     */
    private static McVersion parse(final String version) {
        final Matcher matcher = LEADING_VERSION.matcher(version);
        if (!matcher.lookingAt()) {
            return new McVersion(1, 0, 0);
        }

        final int major = Integer.parseInt(matcher.group(1));
        final int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        final int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new McVersion(major, minor, patch);
    }

    private final int major;
    private final int minor;
    private final int patch;

    public McVersion(final int major, final int minor) {
        this(major, minor, 0);
    }

    public McVersion(final int major, final int minor, final int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Gets the currently running McVersion
     */
    public static McVersion current() {
        return CURRENT_VERSION;
    }

    public boolean isAtLeast(final McVersion other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(final McVersion other) {
        if (this.major > other.major) return 3;
        if (other.major > this.major) return -3;
        if (this.minor > other.minor) return 2;
        if (other.minor > this.minor) return -2;
        return Integer.compare(this.patch, other.patch);
    }

    /**
     * Gets the "major" part of this McVersion. For 1.16.5, this would be 1
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the "minor" part of this McVersion. For 1.16.5, this would be 16
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Gets the "patch" part of this McVersion. For 1.16.5, this would be 5.
     */
    public int getPatch() {
        return patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final McVersion mcVersion = (McVersion) o;
        return major == mcVersion.major && minor == mcVersion.minor && patch == mcVersion.patch;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        if (patch == 0) {
            return major + "." + minor;
        } else {
            return major + "." + minor + "." + patch;
        }
    }

    public boolean isAtLeast(final int major, final int minor, final int patch) {
        return this.isAtLeast(new McVersion(major, minor, patch));
    }

    public boolean isAtLeast(final int major, final int minor) {
        return this.isAtLeast(new McVersion(major, minor));
    }

    /**
     * Checks whether the server version is equal or greater than the given version.
     *
     * @param minorNumber the version to compare the server version with.
     * @return true if the version is equal or newer, otherwise false.
     * @see #CURRENT_VERSION
     * @since 4.0.0
     */
    public static boolean supports(int minorNumber) {
        return CURRENT_VERSION.isAtLeast(1, minorNumber);
    }

    /**
     * Checks whether the server version is equal or greater than the given version.
     *
     * @param minorNumber the version to compare the server version with.
     * @param patchNumber the version to compare the server version with.
     * @return true if the version is equal or newer, otherwise false.
     * @see #CURRENT_VERSION
     * @since 4.0.0
     */
    public static boolean supports(int minorNumber, int patchNumber) {
        return CURRENT_VERSION.isAtLeast(1, minorNumber, patchNumber);
    }

    /**
     * Whether the server is running a "modern" Mojang-mapped NMS naming scheme (1.17+, including
     * the year-based versioning scheme introduced afterwards, e.g. 26.x), as opposed to the legacy
     * obfuscated/versioned CraftBukkit naming scheme used prior to 1.17.
     */
    public static boolean isModern() {
        return CURRENT_VERSION.getMajor() > 1 || CURRENT_VERSION.getMinor() >= 17;
    }
}
