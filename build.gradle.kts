import net.neoforged.gradle.dsl.common.runs.type.RunType
import org.embeddedt.embeddium.gradle.VerifyAPICompat

evaluationDependsOnChildren()

plugins {
    id("idea")
    id("net.neoforged.gradle.userdev")
    id("maven-publish")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

    id("me.modmuss50.mod-publish-plugin") version("0.3.4")

    id("embeddium-fabric-remapper")
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

// Mojang ships Java 21 to end users in 1.20.5+, so your mod should target Java 21.
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val extraSourceSets = arrayOf("legacy", "compat")
val usePhi = rootProject.properties["use_phi"].toString().toBoolean()
val neoForgePr = if(rootProject.hasProperty("neoforge_pr")) rootProject.properties["neoforge_pr"].toString() else null

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
    if(usePhi) {
        maven("https://nexus.gtnewhorizons.com/repository/public/")
    }
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

if(!usePhi) {
    minecraft {
        accessTransformers {
            file(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
        }
    }

    if(project.hasProperty("parchment_version")) {
        val parchment_info = "parchment_version"().split("-")
        subsystems {
            parchment {
                minecraftVersion = parchment_info[1]
                mappingsVersion = parchment_info[0]
            }
        }
    }

    runs {
        // Create the default client run
        create("client")
    }
} else {
    val phiProject = project(":phi")
    runs {
        create("client") {
            configure((phiProject.extensions.getByName("runTypes") as NamedDomainObjectContainer<RunType>).getByName("client"))
        }
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.console.level", "info")

        modSource(sourceSets["main"])
        extraSourceSets.forEach { modSource(sourceSets[it]) }
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

fun DependencyHandlerScope.compatCompileOnly(dependency: String) {
    "compatCompileOnly"(dependency)
}

dependencies {
    if(!usePhi) {
        implementation("net.neoforged:neoforge:${"forge_version"()}")
    } else {
        implementation(project(":phi"))
        // TODO find a less disgusting way of doing this
        //implementation(project(":phi").dependencyProject.tasks.findByPath("create" + "minecraft_version"() + "ClientExtraJar")?.outputs!!.files)
    }

    // FIXME remove when NG not loading this from NF itself is fixed
    implementation("io.github.llamalad7:mixinextras-neoforge:0.3.5")

    // Mods
    compatCompileOnly("curse.maven:codechickenlib-242818:${"codechicken_fileid"()}")

    // Fabric API
    compileOnly("net.fabricmc.fabric-api:fabric-api:${"fabric_version"()}")
    compileOnly("net.fabricmc:fabric-loader:${"fabric_loader_version"()}")
}

tasks.processResources {
    inputs.property("version", "version"())

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to "version"())
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
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
        }
    }

    // Phi builds shouldn't be published to Maven, because they're useless
    if(!usePhi) {
        repositories {
            maven("file://${System.getenv("local_maven")}")
        }
    }
}


tasks.register<DefaultTask>("setupPhi") {
    group = "phi"
    description = "Run setup task in Phi if enabled"
    if(usePhi) {
        dependsOn(":phi:setup")
    }
}

tasks.register<VerifyAPICompat>("verifyAPICompat") {
    group = "verification"
    binary = tasks.getByName<Jar>("jar").archiveFile
}

if(!usePhi) {
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
}

if(usePhi) {
    configurations {
        create("instanceJarConsumer") {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
    }
    dependencies {
        "instanceJarConsumer"(project(":phi", "instanceJar"))
    }
    tasks.register<Zip>("mmcInstanceFiles") {
        dependsOn(project(":phi").tasks.named("mmcInstanceJar"))
        description = "Packages the MultiMC patches"
        archiveClassifier.set("multimc")
        from(project.file("prism-libraries/"))
        from(configurations.getByName("instanceJarConsumer").resolve()) {
            into("libraries")
        }
        exclude("META-INF", "META-INF/**")
        filesMatching(
                listOf(
                        "mmc-pack.json",
                        "patches/org.embeddedt.embeddium.phi.launchargs.json",
                        "patches/org.embeddedt.embeddium.phi.universal.json"
                )
        ) {
            expand(
                    mapOf(
                            "version" to project.version,
                            "minecraftVersion" to "minecraft_version"(),
                            "asmVersion" to "asm_version"(),
                            "mixinVersion" to "mixin_version"(),
                            "mixinExtrasVersion" to "mixin_extras_version"()
                    )
            )
        }
    }
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
