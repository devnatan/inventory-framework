plugins {
    id("me.devnatan.inventoryframework.library")
    alias(libs.plugins.shadowjar)
    alias(libs.plugins.bukkit)
}

inventoryFramework {
    publish = true
    generateVersionFile = true
}

dependencies {
    api(projects.inventoryFrameworkPlatform)
    runtimeOnly(projects.inventoryFrameworkAnvilInput)
    compileOnly(libs.spigot)
    testCompileOnly(libs.spigot)
    testRuntimeOnly(libs.spigot)
    testImplementation(projects.inventoryFrameworkApi)
    testImplementation(projects.inventoryFrameworkTest)
    implementation(libs.folialib)
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