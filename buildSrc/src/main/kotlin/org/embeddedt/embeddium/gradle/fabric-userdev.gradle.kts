import gradle.kotlin.dsl.accessors._e054d9723d982fdb55b1e388b8ab0cbf.sourceSets
import net.fabricmc.loom.configuration.accesstransformer.AccessTransformerJarProcessor
import org.embeddedt.embeddium.gradle.Constants
import org.embeddedt.embeddium.gradle.GenerateAWFromATTask

plugins {
    id("embeddium-common")
    id("dev.architectury.loom")
}

repositories {
    maven("https://maven.neoforged.net") {

    }
    maven("https://maven.parchmentmc.org") {

    }
}

sourceSets {
    val fabric = create("fabric") {
        compileClasspath += getByName("main").compileClasspath
    }
    listOf(*Constants.EXTRA_SOURCE_SETS.toTypedArray(), "main").forEach {
        val set = getByName(it)
        set.compileClasspath += fabric.compileClasspath
        set.compileClasspath += fabric.output
        set.runtimeClasspath += fabric.output
    }
}

loom.addMinecraftJarProcessor(AccessTransformerJarProcessor::class.java, "loom:access-transformer", project, listOf(file("src/main/resources/META-INF/accesstransformer.cfg")))

loom {
    mixin {
        useLegacyMixinAp = false
    }
}

loom {
    mods {
        create("embeddium") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["fabric"])
            Constants.EXTRA_SOURCE_SETS.forEach {
                sourceSet(sourceSets[it])
            }
        }
    }
}

configurations {
    val includeImplementation = create("includeImplementation")

    this["include"].extendsFrom(includeImplementation)
    this["implementation"].extendsFrom(includeImplementation)
}

tasks.create<GenerateAWFromATTask>("convertTransformerToWidener") {
    accessWidenerPath = file("src/main/resources/embeddium.accesswidener")
    accessTransformerPath = file("src/main/resources/META-INF/accesstransformer.cfg")
    overwriteAccessWidener = true
}

tasks.jar {
    from(sourceSets["fabric"].output.classesDirs)
    from(sourceSets["fabric"].output.resourcesDir)
    // strip NeoForge-only assets
    exclude("META-INF/accesstransformer.cfg", "META-INF/neoforge.mods.toml")
}

dependencies {
    fun fAPIModule(name: String): Dependency {
        return fabricApi.module(name, project.properties["fabric_version"].toString())
    }
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"].toString()}")
    mappings(loom.layered {
        officialMojangMappings {
            setNameSyntheticMembers(true)
        }
        if(rootProject.properties.containsKey("parchment_version")) {
            val parchment_version = rootProject.properties["parchment_version"].toString().split("-")
            parchment("org.parchmentmc.data:parchment-${parchment_version[1]}:${parchment_version[0]}@zip")
        }
    })

    "includeImplementation"("net.neoforged:bus:${project.properties["eventbus_version"].toString()}")
    "includeImplementation"("net.jodah:typetools:0.6.3")
    "includeImplementation"("org.apache.maven:maven-artifact:${project.properties["apache_maven_artifact_version"].toString()}")
    "includeImplementation"("net.neoforged:neoforgespi:${project.properties["spi_version"].toString()}")
    "includeImplementation"("net.neoforged:mergetool:2.0.0:forge")

    compileOnly("cpw.mods:modlauncher:${project.properties["modlauncher_version"].toString()}")

    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"].toString()}")
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_version"].toString()}")

    "include"(fAPIModule("fabric-resource-loader-v0"))
    modRuntimeOnly(fAPIModule("fabric-resource-loader-v0"))
}