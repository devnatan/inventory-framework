pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.tcoded.com/releases")
    }
}

rootProject.name = "inventory-framework"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "inventory-framework-test",
    "inventory-framework-api",
    "inventory-framework-core",
    "inventory-framework-platform",
    "inventory-framework-platform-paper",
    "inventory-framework-platform-bukkit",
    "inventory-framework-platform-minestom",
    "inventory-framework-anvil-input",
    "example-paper",
    "example-minestom"
)

project(":example-paper").projectDir = file("examples/paper")
project(":example-minestom").projectDir = file("examples/minestom")
