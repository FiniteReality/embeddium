plugins {
    id("idea")
    id("net.neoforged.gradle") version("6.0.21")
    id("maven-publish")
    id("org.spongepowered.mixin") version("0.7.38")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

    id("me.modmuss50.mod-publish-plugin") version("0.3.4")
}

operator fun String.invoke(): String {
    return (rootProject.properties[this] as String?)!!
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.majorVersion
    targetCompatibility = JavaVersion.VERSION_17.majorVersion
    options.encoding = "UTF-8"
}

version = "${"mod_version"()}${getVersionMetadata()}+mc${"minecraft_version"()}"
group = "maven_group"()

base {
    archivesName = "archives_base_name"()
}

// Mojang ships Java 17 to end users in 1.18+, so your mod should target Java 17.
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

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


minecraft {
    mappings("official", "minecraft_version"())
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
                    sources(sourceSets["compat"])
                    sources(sourceSets["api"])
                    sources(sourceSets["legacy"])
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

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${"archives_base_name"()}"}
    }
    from(sourceSets["compat"].output.classesDirs)
    from(sourceSets["compat"].output.resourcesDir)
    from(sourceSets["api"].output.classesDirs)
    from(sourceSets["api"].output.resourcesDir)
    from(sourceSets["legacy"].output.classesDirs)
    from(sourceSets["legacy"].output.resourcesDir)

    finalizedBy("reobfJar")
}

tasks.named<Jar>("sourcesJar").configure {
    from(sourceSets["compat"].allJava)
    from(sourceSets["api"].allJava)
    from(sourceSets["legacy"].allJava)
}

publishing {
    tasks.publish {
        dependsOn(tasks.build)
    }
    publications {
        this.create<MavenPublication>("mavenJava") {
            artifact(tasks.jar)
            artifact(tasks.named("sourcesJar"))
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

publishMods {
    file = tasks.jar.get().archiveFile
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
    }
    modrinth {
        projectId = "sk9rgfiA"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add("minecraft_version"())

        incompatible {
            slug = "rubidium"
        }
    }

    displayName = "[${"minecraft_version"()}] Embeddium ${"mod_version"()}"
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
