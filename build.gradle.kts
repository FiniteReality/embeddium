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

version = "${"mod_version"()}${getVersionMetadata()}+mc${"minecraft_version"()}"
group = "maven_group"()

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

sourceSets {
    val main = getByName("main")
    val api = create("api")
    val legacy = create("legacy")
    val compat = create("compat")
    
    api.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    main.apply {
        java {
            compileClasspath += api.output
            runtimeClasspath += api.output
        }
    }

    legacy.apply {
        java {
            compileClasspath += main.compileClasspath
            compileClasspath += main.output
        }
    }

    compat.apply {
        java {
            compileClasspath += main.compileClasspath
            compileClasspath += main.output
        }
    }
}

loom {
    runs {
        this["client"].apply {
            mods {
                create("archives_base_name"()) {
                    sourceSet(sourceSets["main"])
                    sourceSet(sourceSets["api"])
                }
            }
        }
    }
    createRemapConfigurations(sourceSets["compat"])
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api-dev"

    from(sourceSets["api"].output)
}

val remapApiJar = tasks.register<RemapJarTask>("remapApiJar") {
    dependsOn(apiJar)
    archiveClassifier = "api"
    
    input = apiJar.get().archiveFile.get().asFile
    addNestedDependencies = false
}

tasks.build {
    dependsOn(apiJar)
    dependsOn(remapApiJar)
}

tasks.jar {
    from(sourceSets["api"].output.classesDirs)
    from(sourceSets["api"].output.resourcesDir)
    from(sourceSets["legacy"].output.classesDirs)
    from(sourceSets["legacy"].output.resourcesDir)
    from(sourceSets["compat"].output.classesDirs)
    from(sourceSets["compat"].output.resourcesDir)
}

java {
    withSourcesJar()
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
    mappings(loom.officialMojangMappings())
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

tasks.named("remapApiJar") {
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
	}

	displayName = "[${"minecraft_version"()}] Embeddium-Fabric ${"mod_version"()}"
}

fun getVersionMetadata(): String {
	// CI builds only
	if (project.hasProperty("build.release")) {
		return "" // no tag whatsoever
	}

    val head = grgit.head()
    var id = head.abbreviatedId

    // Flag the build if the build tree is not clean
    if (!grgit.status().isClean) {
        id += ".dirty"
    }

    return "-git.${id}"
}
