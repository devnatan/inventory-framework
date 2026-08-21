import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("me.devnatan.inventoryframework.library")
    alias(libs.plugins.shadowjar)
    alias(libs.plugins.bukkit)
}

inventoryFramework {
    publish = true
    generateVersionFile = true
}

val folialib: Configuration by configurations.creating

dependencies {
    api(projects.inventoryFrameworkPlatform)
    runtimeOnly(projects.inventoryFrameworkAnvilInput)
    compileOnly(libs.spigot)
    testCompileOnly(libs.spigot)
    testRuntimeOnly(libs.spigot)
    testImplementation(projects.inventoryFrameworkApi)
    testImplementation(projects.inventoryFrameworkTest)
    compileOnly(libs.folialib)
    testRuntimeOnly(libs.folialib)
    folialib(libs.folialib)
}

tasks.replace("jar", ShadowJar::class.java).apply {
    from(sourceSets.main.get().output)
    configurations = listOf(folialib)
    archiveClassifier.set("")
    relocate("com.tcoded.folialib", "me.devnatan.inventoryframework.thirdparty.folialib")
}

tasks.shadowJar {
    archiveBaseName.set("inventory-framework")
    archiveAppendix.set("bukkit")

    relocate("com.tcoded.folialib", "me.devnatan.inventoryframework.thirdparty.folialib")

    dependencies {
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
}

bukkit {
    main = "me.devnatan.inventoryframework.runtime.InventoryFramework"
    name = "InventoryFramework"
    version = project.version.toString()
    description = "Minecraft Inventory API framework"
    website = "https://github.com/DevNatan/inventory-framework"
    apiVersion = "1.13"
    authors = listOf("SaiintBrisson", "DevNatan", "sasuked")
    foliaSupported = true
}