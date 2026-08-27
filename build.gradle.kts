plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
}

group = "me.devnatan"
version = property("version")
    .toString()
    .takeUnless { it == "unspecified" }
    ?.filterNot { it == 'v' } ?: nextGitTag()

@Suppress("UnstableApiUsage")
fun Project.nextGitTag(): String {
    val latestTag = providers.exec {
        workingDir(project.projectDir)
        commandLine("git", "describe", "--tags", "--abbrev=0")
    }.standardOutput.asText.get().trim()

    val versionParts = latestTag.removePrefix("v").split(".")
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

    return "$major.${minor + 1}.0-SNAPSHOT"
}