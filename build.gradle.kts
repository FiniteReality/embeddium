import net.neoforged.moddevgradle.dsl.RunModel
import org.embeddedt.embeddium.gradle.versioning.ProjectVersioner

plugins {
    id("idea")
    id("net.neoforged.moddev.legacy") version("2.0.63-beta-pr-118-legacy")
    id("maven-publish")
    id("me.modmuss50.mod-publish-plugin") version("0.7.4")
    id("embeddium-fabric-remapper")
}

operator fun String.invoke(): String {
    return (rootProject.properties[this] as String?)!!
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.majorVersion
    targetCompatibility = JavaVersion.VERSION_17.majorVersion
    options.encoding = "UTF-8"
}

version = getModVersion()
group = "maven_group"()
println("Embeddium: $version")

base {
    archivesName = "archives_base_name"()
}

// Mojang ships Java 17 to end users in 1.18+, so your mod should target Java 17.
// java.toolchain.languageVersion = JavaLanguageVersion.of(17)

val extraSourceSets = arrayOf("legacy", "compat")

sourceSets {
    val main = getByName("main")

    extraSourceSets.forEach {
        val sourceset = create(it)
        sourceset.apply {
            java {
                compileClasspath += main.compileClasspath
                compileClasspath += main.output
            }
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    maven("https://maven.tterrag.com/")
    maven("https://maven.blamejared.com")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://maven.covers1624.net/")
}

neoForge {
    version = "${"minecraft_version"()}-${"forge_version"()}"
    if(rootProject.properties.containsKey("parchment_version")) {
        val parchment_info = rootProject.properties["parchment_version"].toString().split("-")
        parchment {
            minecraftVersion = parchment_info[1]
            mappingsVersion = parchment_info[0]
        }
    }
    mods {
        create("embeddium") {
            sourceSet(sourceSets["main"])
            extraSourceSets.forEach {
                sourceSet(sourceSets[it])
            }
        }
    }
    runs {
        configureEach {
            systemProperty("forge.logging.console.level", "info")

            systemProperty("mixin.env.remapRefMap", "true")
            systemProperty("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods.add(neoForge.mods.named("embeddium"))
        }

        create("client") {
            client()
        }

        fun configureGameTestRun(run: RunModel) {
            run.client()
            run.systemProperty("embeddium.enableGameTest", "true")
            run.systemProperty("forge.enableGameTest", "true")
        }

        create("gameTestClient") {
            configureGameTestRun(this)
        }

        create("gameTestCiClient") {
            configureGameTestRun(this)
            systemProperty("embeddium.runAutomatedTests", "true")
        }
    }
}

configurations {
    val runtimeOnlyNonPublishable = create("runtimeOnlyNonPublishable") {
        description = "Runtime only dependencies that are not published alongside the jar"
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    runtimeClasspath.get().extendsFrom(runtimeOnlyNonPublishable)
}

mixin {
    // MixinGradle Settings
    add(sourceSets["main"], "embeddium-refmap.json")
    config("embeddium.mixins.json")
}

obfuscation {
    createRemappingConfiguration(configurations.getByName("compatCompileOnly"))
}

fun fAPIModule(name: String): Dependency {
    return fabricApiModuleFinder.module(name, "fabric_version"())
}

dependencies {
    // Mods
    "modCompatCompileOnly"("curse.maven:codechickenlib-242818:${"codechicken_fileid"()}")
    "modCompatCompileOnly"("curse.maven:immersiveengineering-231951:${"ie_fileid"()}")
    "modCompatCompileOnly"("com.brandon3055.brandonscore:BrandonsCore:1.20.1-3.2.1.302:universal")

    // Fabric API
    compileOnly("org.jetbrains:annotations:24.1.0")
    "fabricCompileOnly"(fAPIModule("fabric-api-base"))
    "fabricCompileOnly"(fAPIModule("fabric-block-view-api-v2"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-api-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-rendering-data-attachment-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-indigo"))
    compileOnly("net.fabricmc:fabric-loader:${"fabric_loader_version"()}")

    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")

    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    jarJar("io.github.llamalad7:mixinextras-forge:0.3.5") {
        version {
            strictly("[0.3.5,)")
            prefer("0.3.5")
        }
    }
    implementation("io.github.llamalad7:mixinextras-forge:0.3.5")
}

tasks.processResources {
    inputs.property("version", "version"())

    filesMatching("META-INF/mods.toml") {
        expand("file" to mapOf("jarVersion" to inputs.properties["version"]))
    }
}

tasks.withType<JavaCompile> {
    options.release = 17
}

java {
    withSourcesJar()
}

tasks.named<Jar>("jar").configure {
    archiveClassifier = "slim"

    from("COPYING", "COPYING.LESSER", "README.md")

    extraSourceSets.forEach {
        from(sourceSets[it].output.classesDirs)
        from(sourceSets[it].output.resourcesDir)
    }

    manifest {
        attributes["MixinConfigs"] = mixin.configs.map { it.joinToString(",") }
    }
}

tasks.named<Jar>("sourcesJar").configure {
    extraSourceSets.forEach {
        from(sourceSets[it].allJava)
    }
}

publishing {
    tasks.publish {
        dependsOn(tasks.build)
    }
    publications {
        this.create<MavenPublication>("mavenJava") {
            artifact(tasks.named("reobfJar"))
            artifact(tasks.named("sourcesJar"))
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

publishMods {
    file = tasks.reobfJar.get().archiveFile
    changelog = "https://github.com/embeddedt/embeddium/wiki/Changelog"
    type = STABLE
    modLoaders.add("forge")
    modLoaders.add("neoforge")

    curseforge {
        projectId = "908741"
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add("minecraft_version"())

        incompatible {
            slug = "rubidium"
        }

        incompatible {
            slug = "textrues-embeddium-options"
        }
    }
    modrinth {
        projectId = "sk9rgfiA"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add("minecraft_version"())

        incompatible {
            slug = "rubidium"
        }

        incompatible {
            slug = "textrues-embeddium-options"
        }
    }

    displayName = "[${"minecraft_version"()}] Embeddium ${"mod_version"()}"
}

fun getModVersion(): String {
    return ProjectVersioner.computeVersion(project.projectDir, project.properties)
}
