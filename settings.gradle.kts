import net.neoforged.gradle.platform.extensions.DynamicProjectManagementExtension

pluginManagement {
    repositories {
        maven("https://maven.neoforged.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        mavenCentral()
        gradlePluginPortal()
    }
}

val minecraft_version: String by settings
val use_phi = extra["use_phi"].toString().toBoolean()

plugins {
    id("net.neoforged.gradle.platform") version("7.0.142")
    id("net.neoforged.gradle.userdev") version("7.0.142") apply false
}

if(use_phi) {
    extensions.getByType(DynamicProjectManagementExtension::class).include(":phi")
}

rootProject.name = "embeddium-${minecraft_version}"
