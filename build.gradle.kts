import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("fabric-loom") version("1.5.7")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

	id("me.modmuss50.mod-publish-plugin") version "0.3.4"

    id("maven-publish")
}

operator fun String.invoke(): String {
    return (rootProject.properties[this] as String?)!!
}

base {
    archivesName = "archives_base_name"()
}

version = getModVersion()
group = "maven_group"()
println("Embeddium: $version")

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.majorVersion
    targetCompatibility = JavaVersion.VERSION_17.majorVersion
    options.encoding = "UTF-8"
}

tasks.jar {
    from("${rootProject.projectDir}/LICENSE.txt")
}

loom {
    mixin {
        defaultRefmapName = "embeddium-refmap.json"
    }

    accessWidenerPath = file("src/main/resources/sodium.accesswidener")
}

configurations {
    val modIncludeImplementation = create("modIncludeImplementation")

    this["include"].extendsFrom(modIncludeImplementation)
    this["modImplementation"].extendsFrom(modIncludeImplementation)
    
    val modIncludeRuntime = create("modIncludeRuntime")
    
    this["include"].extendsFrom(modIncludeRuntime)
    this["modRuntimeOnly"].extendsFrom(modIncludeRuntime)
}

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

loom {
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

java {
    withSourcesJar()
}

tasks.jar {
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

repositories {
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://maven.parchmentmc.org") {

    }
}

dependencies {
    fun fAPIModule(name: String): Dependency {
        return fabricApi.module(name, "fabric_version"())
    }

    fun stub(name: String) {
        runtimeOnly(project(":stub:${name}"))
        include(project(":stub:${name}"))
    }
    
    //to change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${"minecraft_version"()}")
    mappings(loom.layered {
        officialMojangMappings()
        if(rootProject.properties.containsKey("parchment_version")) {
            val parchment_version = "parchment_version"().split("-")
            parchment("org.parchmentmc.data:parchment-${parchment_version[1]}:${parchment_version[0]}@zip")
        }
    })
    modImplementation("net.fabricmc:fabric-loader:${"loader_version"()}")

    // Fabric API
    "modIncludeImplementation"(fAPIModule("fabric-api-base"))
    "modIncludeImplementation"(fAPIModule("fabric-block-view-api-v2"))
    "modIncludeImplementation"(fAPIModule("fabric-rendering-fluids-v1"))
    "modIncludeImplementation"(fAPIModule("fabric-rendering-data-attachment-v1"))
    "modIncludeImplementation"(fAPIModule("fabric-resource-loader-v0"))
    "modIncludeImplementation"(fAPIModule("fabric-renderer-api-v1"))
    "modIncludeImplementation"(fAPIModule("fabric-renderer-indigo"))

    // Include Zume in dev
    modRuntimeOnly(fAPIModule("fabric-key-binding-api-v1"))
    modRuntimeOnly("maven.modrinth:zume:${"zume_version"()}")

    // provide mod IDs of former mods/addons
    stub("sodium")
    stub("indium")
}

val remapJar = tasks.withType<RemapJarTask>()["remapJar"]!!
val remapSourcesJar = tasks.withType<RemapSourcesJarTask>()["remapSourcesJar"]!!

val copyJarNameConsistent = tasks.register<Copy>("copyJarNameConsistent") {
    from(remapJar) // shortcut for createJar.outputs.files
    into(project.file("build/libs"))
    rename { "embeddium-fabric-latest.jar" }
}

val copyJarToBin = tasks.register<Copy>("copyJarToBin") {
    from(remapJar) // shortcut for createJar.outputs.files
    into(rootProject.file("bin"))
    mustRunAfter(copyJarNameConsistent)
}

tasks.named("remapSourcesJar") {
    mustRunAfter(copyJarNameConsistent)
}

tasks.build {
    dependsOn(copyJarToBin, copyJarNameConsistent)
}

publishing {
    tasks.publish {
        dependsOn(tasks.build)
    }
    publications {
        this.create<MavenPublication>("mavenJava") {
            artifact(tasks.named("remapJar"))
            artifact(tasks.named("remapSourcesJar"))
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

publishMods {
	file = remapJar.archiveFile
	changelog = "https://github.com/embeddedt/embeddium/wiki/Changelog"
	type = STABLE
    modLoaders.add("fabric")

	curseforge {
		projectId = "908741"
		accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
		minecraftVersions.add("minecraft_version"())

		incompatible {
			slug = "sodium"
		}
		incompatible {
			slug = "indium"
		}
        incompatible {
            slug = "reeses-sodium-options"
        }
	}
	modrinth {
		projectId = "sk9rgfiA"
		accessToken = providers.environmentVariable("MODRINTH_TOKEN")
		minecraftVersions.add("minecraft_version"())

		incompatible {
			slug = "sodium"
		}
        incompatible {
            slug = "indium"
        }
        incompatible {
            slug = "reeses-sodium-options"
        }
	}

	displayName = "[${"minecraft_version"()}] Embeddium-Fabric ${"mod_version"()}"
}

fun getModVersion(): String {
    var baseVersion: String = project.properties["mod_version"].toString()
    val mcMetadata: String = "+mc" + project.properties["minecraft_version"]

    if (project.hasProperty("build.release")) {
        return baseVersion + mcMetadata // no tag whatsoever
    }

    // Increment patch version
    baseVersion = baseVersion.split(".").mapIndexed {
        index, s -> if(index == 2) (s.toInt() + 1) else s
    }.joinToString(separator = ".")

    val head = grgit.head()
    var id = head.abbreviatedId

    // Flag the build if the build tree is not clean
    if (!grgit.status().isClean) {
        id += "-dirty"
    }

    return baseVersion + "-git-${id}" + mcMetadata
}
