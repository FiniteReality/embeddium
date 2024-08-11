import org.embeddedt.embeddium.gradle.Constants
import net.neoforged.gradle.dsl.common.runs.run.Run

plugins {
    id("embeddium-common")
    id("net.neoforged.gradle.userdev")
}

apply(plugin = "embeddium-fabric-remapper")

val neoForgePr = if(rootProject.hasProperty("neoforge_pr")) rootProject.properties["neoforge_pr"].toString() else null

sourceSets {
    val main = getByName("main")

    create("gameTest") {
        compileClasspath += main.compileClasspath
        compileClasspath += main.output
    }
}

repositories {
    if(neoForgePr != null) {
        maven("https://prmaven.neoforged.net/NeoForge/pr" + neoForgePr) {
            content {
                includeModule("net.neoforged", "neoforge")
                includeModule("net.neoforged", "testframework")
            }
        }
    }
}

// FIXME hack to prevent LWJGL from being resolved from the Neo maven, which doesn't contain all needed variants
repositories["NeoForm Maven"].content {
    excludeGroupByRegex("org\\.lwjgl.*")
}

minecraft {
    accessTransformers {
        file(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }
}

if(project.hasProperty("parchment_version")) {
    val parchment_info = project.properties["parchment_version"].toString().split("-")
    subsystems {
        parchment {
            minecraftVersion = parchment_info[1]
            mappingsVersion = parchment_info[0]
        }
    }
}

runs {
    // Create the default client run
    val client = create("client")

    fun configureGameTestRun(run: Run) {
        run.gameTest(true)
        run.modSource(sourceSets["gameTest"])
    }

    create("gameTestClient") {
        configure("client")
        configureGameTestRun(this)
    }

    create("gameTestCiClient") {
        configure("client")
        configureGameTestRun(this)
        systemProperty("embeddium.runAutomatedTests", "true")
    }
}

tasks.jar {
    // strip Fabric-only assets
    exclude("fabric.mod.json", "embeddium.accesswidener")
}

runs {
    configureEach {
        systemProperty("forge.logging.console.level", "info")

        modSource(sourceSets["main"])
        Constants.EXTRA_SOURCE_SETS.forEach { modSource(sourceSets[it]) }
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${project.properties["forge_version"].toString()}")

    // Fabric API
    compileOnly("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_version"].toString()}")
    compileOnly("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"].toString()}")
}