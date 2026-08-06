plugins {
    id("me.devnatan.inventoryframework.library")
}

inventoryFramework {
    publish = true
}

dependencies {
    compileOnly(libs.spigot)
    testCompileOnly(libs.spigot)
    testRuntimeOnly(libs.spigot)
    compileOnlyApi(projects.inventoryFrameworkPlatformBukkit)
    testImplementation(projects.inventoryFrameworkPlatformBukkit)
}
