import org.embeddedt.embeddium.gradle.Constants
import org.embeddedt.embeddium.gradle.fabric.remapper.FabricApiModuleFinder
import net.neoforged.gradle.dsl.common.runs.run.Run

plugins {
    id("embeddium-common")
    id("net.neoforged.gradle.userdev")
}

apply(plugin = "embeddium-fabric-remapper")

val neoForgePr = if(rootProject.hasProperty("neoforge_pr")) rootProject.properties["neoforge_pr"].toString() else null

sourceSets {
    val main = getByName("main")

    main.java.srcDirs("src/gametest/java")
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
        run.systemProperty("embeddium.enableGameTest", "true")
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

fun fAPIModule(name: String): Dependency {
    return the<FabricApiModuleFinder>().module(name, project.properties["fabric_version"].toString())
}

dependencies {
    implementation("net.neoforged:neoforge:${project.properties["forge_version"].toString()}")

    // Fabric API
    "fabricCompileOnly"(fAPIModule("fabric-api-base"))
    "fabricCompileOnly"(fAPIModule("fabric-block-view-api-v2"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-api-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-rendering-data-attachment-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-indigo"))
    compileOnly("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"].toString()}")
}
