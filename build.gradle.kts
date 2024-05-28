import org.w3c.dom.Element

plugins {
    id("idea")
    id("net.minecraftforge.gradle") version("6.0.25")
    id("maven-publish")
    id("org.spongepowered.mixin") version("0.7.38")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

    id("me.modmuss50.mod-publish-plugin") version("0.3.4")

    id("org.parchmentmc.librarian.forgegradle") version("1.2.0.7-dev-SNAPSHOT")

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
}

jarJar.enable()

minecraft {
    if(rootProject.properties.containsKey("parchment_version")) {
        mappings("parchment", "parchment_version"())
    } else {
        mappings("official", "minecraft_version"())
    }
    copyIdeResources = true
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
    runs {
        configureEach {
            workingDirectory(project.file("run"))

            property("forge.logging.console.level", "info")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")

            mods {
                create("embeddium") {
                    sources(sourceSets["main"])
                    extraSourceSets.forEach {
                        sources(sourceSets[it])
                    }
                }
            }
        }

        create("client") {}
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

val extraModsDir = "extra-mods-${"minecraft_version"()}"

repositories {
    flatDir {
        name = "extra-mods"
        dirs(file(extraModsDir))
    }
}

mixin {
    // MixinGradle Settings
    add(sourceSets["main"], "embeddium-refmap.json")
    config("embeddium.mixins.json")
}

fun DependencyHandlerScope.compatCompileOnly(dependency: Dependency) {
    "compatCompileOnly"(dependency)
}

dependencies {
    minecraft("net.minecraftforge:forge:${"minecraft_version"()}-${"forge_version"()}")

    // Mods
    compatCompileOnly(fg.deobf("curse.maven:codechickenlib-242818:${"codechicken_fileid"()}"))
    compatCompileOnly(fg.deobf("curse.maven:immersiveengineering-231951:${"ie_fileid"()}"))

    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")

    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.3.5")) {
        jarJar.ranged(this, "[0.3.5,)")
    }

    // runtime remapping at home
    fileTree(extraModsDir) {
        include("*.jar") 
    }.files.forEach { extraModJar ->
        val basename = extraModJar.name.substring(0, extraModJar.name.length - ".jar".length)
        val versionSep = basename.lastIndexOf('-')
        assert(versionSep != -1)
        val artifactId = basename.substring(0, versionSep)
        val version = basename.substring(versionSep + 1)
        runtimeOnly(fg.deobf("extra-mods:$artifactId:$version"))
    }
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

tasks.named<Jar>("jar").configure {
    archiveClassifier = "slim"
}

tasks.jarJar {
    from("COPYING", "COPYING.LESSER", "README.md")

    extraSourceSets.forEach {
        from(sourceSets[it].output.classesDirs)
        from(sourceSets[it].output.resourcesDir)
    }

    finalizedBy("reobfJarJar")

    archiveClassifier = ""
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
            artifact(tasks.named("jarJar"))
            artifact(tasks.named("sourcesJar"))
            fg.component(this)
            pom {
                withXml {
                    // Workaround for NG only checking for net.minecraftforge group
                    val root = this.asElement()

                    val depsParent = (root.getElementsByTagName("dependencies").item(0) as Element)
                    val allDeps = depsParent.getElementsByTagName("dependency")

                    (0..allDeps.length).map { allDeps.item(it) }.filterIsInstance<Element>().filter {
                        val artifactId = it.getElementsByTagName("artifactId").item(0).textContent
                        val groupId = it.getElementsByTagName("groupId").item(0).textContent
                        (artifactId == "forge") && (groupId == "net.neoforged")
                    }.forEach {
                        depsParent.removeChild(it)
                    }
                }
            }
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

publishMods {
    file = tasks.jarJar.get().archiveFile
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