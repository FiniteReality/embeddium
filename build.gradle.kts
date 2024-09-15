import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.embeddedt.embeddium.gradle.versioning.ProjectVersioner
import org.w3c.dom.Element

plugins {
    id("idea")
    id("dev.architectury.loom") version("1.6.397")
    id("maven-publish")

    id("me.modmuss50.mod-publish-plugin") version("0.3.4")

    id("embeddium-fabric-remapper")

    id("com.gradleup.shadow") version "8.3.0"
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
    maven("https://maven.minecraftforge.net/")
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
    maven("https://maven.parchmentmc.org") {

    }
}

configurations {
    val runtimeOnlyNonPublishable = create("runtimeOnlyNonPublishable") {
        description = "Runtime only dependencies that are not published alongside the jar"
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    runtimeClasspath.get().extendsFrom(runtimeOnlyNonPublishable)
    implementation.get().extendsFrom(getByName("shadow"))
}

val extraModsDir = "extra-mods-${"minecraft_version"()}"

repositories {
    flatDir {
        name = "extra-mods"
        dirs(file(extraModsDir))
    }
}

fun DependencyHandlerScope.compatCompileOnly(dependency: Any) {
    "compatCompileOnly"(dependency)
}

loom {
    forge {
        mixinConfigs.add("embeddium.mixins.json")
    }
    mixin.defaultRefmapName = "embeddium-refmap.json"
    runs {
        this["client"].apply {
            mods {
                create("archives_base_name"()) {
                    sourceSet(sourceSets["main"])
                    extraSourceSets.forEach {
                        sourceSet(sourceSets[it])
                    }
                }
            }
        }
    }
    createRemapConfigurations(sourceSets["compat"])
}

fun fAPIModule(name: String): Dependency {
    return fabricApiModuleFinder.module(name, "fabric_version"())
}

dependencies {
    minecraft("com.mojang:minecraft:${"minecraft_version"()}")
    mappings(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
        if(rootProject.properties.containsKey("parchment_version")) {
            val parchment_version = "parchment_version"().split("-")
            parchment("org.parchmentmc.data:parchment-${parchment_version[1]}:${parchment_version[0]}@zip")
        }
    })
    forge("net.minecraftforge:forge:${"minecraft_version"()}-${"forge_version"()}")

    // Mods
    "modCompatCompileOnly"("curse.maven:codechickenlib-242818:${"codechicken_fileid"()}")
    "modCompatCompileOnly"("curse.maven:flywheel-486392:3535459")

    modLocalRuntime("curse.maven:lazydfu-460819:3249059")

    // Fabric API
    "fabricCompileOnly"(fAPIModule("fabric-api-base"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-api-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-rendering-data-attachment-v1"))
    "fabricCompileOnly"(fAPIModule("fabric-renderer-indigo"))
    compileOnly("net.fabricmc:fabric-loader:${"fabric_loader_version"()}")

    //"runtimeOnlyNonPublishable"(fg.deobf("curse.maven:modernfix-790626:5288170"))

    shadow("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    shadow("org.joml:joml:1.10.5")
}

tasks.processResources {
    inputs.property("version", "version"())

    filesMatching("META-INF/mods.toml") {
        expand("version" to "version"())
    }
}

tasks.withType<JavaCompile> {
    options.release = 17
}

java {
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    from("COPYING", "COPYING.LESSER", "README.md")

    extraSourceSets.forEach {
        from(sourceSets[it].output.classesDirs)
        from(sourceSets[it].output.resourcesDir)
    }
}

tasks.named<Jar>("sourcesJar").configure {
    extraSourceSets.forEach {
        from(sourceSets[it].allJava)
    }
}

tasks.named<ShadowJar>("shadowJar").configure {
    archiveClassifier = "dev-shadow"
    configurations = listOf(project.configurations.shadow.get())
    relocate("com.llamalad7.mixinextras", "org.embeddedt.embeddium.impl.shadow.mixinextras")
    relocate("org.joml", "org.embeddedt.embeddium.impl.shadow.joml")
    mergeServiceFiles()
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn("shadowJar")
    archiveClassifier = ""
    inputFile = tasks.getByName<ShadowJar>("shadowJar").archiveFile.get()
}

publishing {
    tasks.publish {
        dependsOn(tasks.build)
    }
    publications {
        this.create<MavenPublication>("mavenJava") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("remapSourcesJar"))
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

publishMods {
    file = tasks.shadowJar.get().archiveFile
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
