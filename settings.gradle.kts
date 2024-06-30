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
    }
}
val minecraft_version: String by settings
rootProject.name = "embeddium-${minecraft_version}"
