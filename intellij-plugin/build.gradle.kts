import java.util.Properties

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform")
}

val pluginProperties = Properties().apply {
    file("gradle.properties").inputStream().use { load(it) }
}

fun pluginProperty(name: String): String =
    pluginProperties.getProperty(name) ?: error("Missing property '$name' in intellij-plugin/gradle.properties")

group = pluginProperty("pluginGroup")
version = pluginProperty("pluginVersion")

dependencies {
    compileOnly(projects.inventoryFrameworkApi)

    intellijPlatform {
        intellijIdea(pluginProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = pluginProperty("pluginName")
        version = pluginProperty("pluginVersion")

        ideaVersion {
            sinceBuild = pluginProperty("pluginSinceBuild")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(pluginProperty("javaVersion"))
    }
}

kotlin {
    jvmToolchain(pluginProperty("javaVersion").toInt())
}
