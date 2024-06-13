
val fabricMode = if(rootProject.hasProperty("embeddium.fabric_mode")) rootProject.properties["embeddium.fabric_mode"].toString().toBoolean() else false

if(fabricMode) {
    apply(plugin = "fabric-userdev")
} else {
    apply(plugin = "neoforge-userdev")
}