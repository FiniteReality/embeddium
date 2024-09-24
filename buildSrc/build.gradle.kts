plugins {
    `java-gradle-plugin` // so we can assign and ID to our plugin
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation("net.fabricmc:mapping-io:0.6.1")
    implementation("net.fabricmc:mapping-io-extras:0.6.1")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    implementation("net.neoforged.gradle:userdev:7.0.165") // NeoGradle
    implementation("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.6.397") // Loom
    implementation("com.github.johnrengelman:shadow:8.1.1") // Shadow
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.neoforged.net")
}

gradlePlugin {
    plugins {
        // here we register our plugin with an ID
        register("embeddium-fabric-remapper") {
            id = "embeddium-fabric-remapper"
            implementationClass = "org.embeddedt.embeddium.gradle.fabric.remapper.RemapperPlugin"
        }
    }
}
