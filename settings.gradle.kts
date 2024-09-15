pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            url = uri("https://maven.architectury.dev/")
        }
        gradlePluginPortal()
        maven("https://prmaven.neoforged.net/ModDevGradle/pr118") {
            name = "Maven for PR #118"
            content {
                includeModule("net.neoforged", "moddev-gradle")
                includeModule("net.neoforged.moddev", "net.neoforged.moddev.gradle.plugin")
                includeModule("net.neoforged.moddev.repositories", "net.neoforged.moddev.repositories.gradle.plugin")
                includeModule("net.neoforged.moddev.legacy", "net.neoforged.moddev.legacy.gradle.plugin")
            }
        }
    }
}
val minecraft_version: String by settings
rootProject.name = "embeddium-${minecraft_version}"
