import org.embeddedt.embeddium.gradle.VerifyAPICompat

evaluationDependsOnChildren()

plugins {
    id("idea")
    id("java-library")
    id("maven-publish")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

    id("me.modmuss50.mod-publish-plugin") version("0.3.4")

    id("embeddium-platform-selector")
}

operator fun String.invoke(): String {
    return (rootProject.properties[this] as String?)!!
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_21.majorVersion
    targetCompatibility = JavaVersion.VERSION_21.majorVersion
    options.encoding = "UTF-8"
}

version = getModVersion()
group = "maven_group"()
println("Embeddium: $version")

base {
    archivesName = "archives_base_name"()
}

repositories {
    maven("https://libraries.minecraft.net")
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

configurations {
    val runtimeOnlyNonPublishable = create("runtimeOnlyNonPublishable") {
        description = "Runtime only dependencies that are not published alongside the jar"
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    runtimeClasspath.get().extendsFrom(runtimeOnlyNonPublishable)
}

fun DependencyHandlerScope.compatCompileOnly(dependency: String) {
    "compatCompileOnly"(dependency)
}

dependencies {
    // FIXME remove when NG not loading this from NF itself is fixed
    implementation("io.github.llamalad7:mixinextras-neoforge:0.3.5")

    // Mods
    compatCompileOnly("curse.maven:codechickenlib-242818:${"codechicken_fileid"()}")
}

tasks.processResources {
    inputs.property("version", "version"())

    filesMatching(listOf("META-INF/neoforge.mods.toml", "fabric.mod.json")) {
        expand("version" to "version"())
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
}


java {
    withSourcesJar()
}

// Make a JAR that has impl stripped
tasks.create<Jar>("apiJar") {
    archiveClassifier = "api"
    from(sourceSets["main"].output) {
        exclude("org/embeddedt/embeddium/impl/**")
        exclude("assets/**")
    }
}

publishing {
    tasks.publish {
        dependsOn(tasks.build)
    }
    publications {
        this.create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("apiJar"))
        }
    }
}

tasks.register<VerifyAPICompat>("verifyAPICompat") {
    group = "verification"
    binary = tasks.getByName<Jar>("jar").archiveFile
}

publishMods {
    file = tasks.jar.get().archiveFile
    changelog = "https://github.com/embeddedt/embeddium/wiki/Changelog"
    type = STABLE
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
