plugins {
    id("me.devnatan.inventoryframework.library")
    alias(libs.plugins.shadowjar)
}

inventoryFramework {
    publish = true
}

dependencies {
    compileOnly(libs.paperSpigot)
    implementation(projects.inventoryFrameworkPlatformBukkit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.shadowJar {
    archiveBaseName.set("inventory-framework")
    archiveAppendix.set("paper")

    dependencies {
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
}