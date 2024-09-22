import org.embeddedt.embeddium.gradle.VerifyAPICompat
import org.embeddedt.embeddium.gradle.versioning.ProjectVersioner

evaluationDependsOnChildren()

plugins {
    id("idea")
    id("java-library")
    id("maven-publish")

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

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
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
    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}

tasks.register<VerifyAPICompat>("verifyAPICompat") {
    group = "verification"
    binary = tasks.getByName<Jar>("jar").archiveFile
}

publishMods {
    file = tasks.jar.get().archiveFile
    changelog = "https://github.com/embeddedt/embeddium/wiki/Changelog"
    val modVer = "mod_version"()
    if(modVer.contains("beta")) {
        type = BETA
    } else if(modVer.contains("alpha")) {
        type = ALPHA
    } else {
        type = STABLE
    }
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
