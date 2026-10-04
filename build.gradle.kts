plugins {
    alias(libs.plugins.loom)
}

// --- Property accessors --------------------------------------------------

class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
    val description = property("mod.description").toString()
    val author = property("mod.author").toString()
    val license = property("mod.license").toString()
    val source = property("mod.source").toString()
    val issues = property("mod.issues").toString()
}

class Dependencies {
    val neoforgeVersion = findProperty("deps.neoforge_version")
    val fabricLoaderVersion = property("deps.fabric_loader_version")
    val fabricApiVersion = findProperty("deps.fabric_api_version")
}

class LoaderData {
    val loader = loom.platform.get().name.lowercase()
    val isFabric = loader == "fabric"
    val isNeoforge = loader == "neoforge"
}

class McData {
    val version = property("mod.mc_version").toString()
    val dep = property("mod.mc_dep").toString()
}

val mc = McData()
val mod = ModData()
val deps = Dependencies()
val loader = LoaderData()

version = "${mod.version}+${mc.version}-${loader.loader}"
group = mod.group
base { archivesName.set(mod.id) }

// --- Stonecutter preprocessor constants ----------------------------------
// Use in sources as:  //? if fabric { ... //?}  or  //? if neoforge { ... //?}
stonecutter {
    constants["fabric"] = loader.isFabric
    constants["neoforge"] = loader.isNeoforge
}

loom {
    silentMojangMappingsLicense()

    // Keep all run configs pointed at one shared run folder across versions.
    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        runDir = "../../run"
    }
}

repositories {
    maven("https://maven.parchmentmc.org")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.fabricmc.net")
    maven("https://api.modrinth.com/maven") // Create & other mod deps (added later)
}

dependencies {
    minecraft("com.mojang:minecraft:${mc.version}")

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        // Parchment adds parameter names + javadoc on top of Mojmap.
        optionalProp("deps.parchment_version") {
            parchment("org.parchmentmc.data:parchment-${mc.version}:$it@zip")
        }
    })

    if (loader.isFabric) {
        modImplementation("net.fabricmc:fabric-loader:${deps.fabricLoaderVersion}")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${deps.fabricApiVersion}+${mc.version}")
    } else if (loader.isNeoforge) {
        "neoForge"("net.neoforged:neoforge:${deps.neoforgeVersion}")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

// --- Metadata templating -------------------------------------------------

tasks.processResources {
    val props = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "mcdep" to mc.dep,
        "description" to mod.description,
        "author" to mod.author,
        "license" to mod.license,
        "source" to mod.source,
        "issues" to mod.issues,
        "fabric_loader_version" to deps.fabricLoaderVersion,
        "neoforge_version" to (deps.neoforgeVersion ?: "")
    )

    props.forEach { (k, v) -> inputs.property(k, v) }

    if (loader.isFabric) {
        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/neoforge.mods.toml")
    } else if (loader.isNeoforge) {
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        exclude("fabric.mod.json")
    }
}

// Convenience alias: build only the currently active target.
if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(tasks.named("build"))
    }
}

fun <T> optionalProp(property: String, block: (String) -> T?): T? =
    findProperty(property)?.toString()?.takeUnless { it.isBlank() }?.let(block)
