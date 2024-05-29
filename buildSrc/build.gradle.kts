plugins {
    `java-gradle-plugin` // so we can assign and ID to our plugin
}

dependencies {
    implementation("net.fabricmc:mapping-io:0.6.1")
    implementation("net.fabricmc:mapping-io-extras:0.6.1")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
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
