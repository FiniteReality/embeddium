import net.neoforged.gradle.platform.extensions.DynamicProjectManagementExtension

pluginManagement {
    repositories {
        maven("https://maven.neoforged.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        // Add the maven repository for the ModDevGradle plugin.
        maven("https://prmaven.neoforged.net/ModDevGradle/pr1") {
            name = "Maven for PR #1" // https://github.com/neoforged/ModDevGradle/pull/1
            content {
                includeModule("net.neoforged.moddev", "net.neoforged.moddev.gradle.plugin")
                includeModule("net.neoforged.moddev.junit", "net.neoforged.moddev.junit.gradle.plugin")
                includeModule("net.neoforged", "moddev-gradle")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val minecraft_version: String by settings
val use_phi = extra["use_phi"].toString().toBoolean()

plugins {
    id("net.neoforged.gradle.platform") version("7.0.134")
    id("net.neoforged.moddev") version("0.1.32-pr-1-pr-publish") apply false
}

if(use_phi) {
    extensions.getByType(DynamicProjectManagementExtension::class).include(":phi")
}

rootProject.name = "embeddium-${minecraft_version}"
