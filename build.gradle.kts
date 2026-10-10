plugins {
    alias(libs.plugins.loom)
}

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
    val neoforgeVersion = property("deps.neoforge_version").toString()
    val createVersion = property("deps.create_version").toString()
    val createMin = property("deps.create_min").toString()
    val tfmgVersion = property("deps.tfmg_version").toString()
    val tfmgMin = property("deps.tfmg_min").toString()
    val principlesVersion = property("deps.principles_version").toString()
    val principlesMin = property("deps.principles_min").toString()
    val chemicaVersion = property("deps.chemica_version").toString()
}

class McData {
    val version = property("mod.mc_version").toString()
    val dep = property("mod.mc_dep").toString()
}

val mc = McData()
val mod = ModData()
val deps = Dependencies()

version = "${mod.version}+${mc.version}"
group = mod.group
base { archivesName.set(mod.id) }

loom {
    silentMojangMappingsLicense()

    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        runDir = "../../run"
        // Makes `/test runall` available in dev runs.
        vmArg("-Dneoforge.enabledGameTestNamespaces=${mod.id}")
    }
    // Opens the dev client straight into run/saves/showcase, a staged world for looking at the blocks.
    runs.create("showcase") {
        client()
        programArgs("--quickPlaySingleplayer", "showcase")
    }
}

repositories {
    maven("https://maven.parchmentmc.org")
    maven("https://maven.neoforged.net/releases")
    maven("https://api.modrinth.com/maven") // Create
    maven("https://maven.createmod.net")         // the libraries Create bundles: Ponder, Flywheel
    maven("https://maven.ithundxr.dev/snapshots") // ...and Registrate
    exclusiveContent {
        // Fundamentals: Principles, from `./gradlew publishToMavenLocal` in its checkout
        forRepository { mavenLocal() }
        filter { includeGroup("com.wildspell.fundamental") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${mc.version}")

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        optionalProp("deps.parchment_version") {
            parchment("org.parchmentmc.data:parchment-${mc.version}:$it@zip")
        }
    })

    "neoForge"("net.neoforged:neoforge:${deps.neoforgeVersion}")
    modCompileOnly("maven.modrinth:create:${deps.createVersion}")
    modCompileOnly("maven.modrinth:create-tfmg:${deps.tfmgVersion}")
    // bundled inside our jar, so players never install it separately
    include(modImplementation("com.wildspell.fundamental:fundamentalmagic:${deps.principlesVersion}")!!)
    modLocalRuntime("maven.modrinth:create:${deps.createVersion}")
    modLocalRuntime("maven.modrinth:create-tfmg:${deps.tfmgVersion}")
    modCompileOnly("maven.modrinth:create:${deps.createVersion}")
    modCompileOnly("maven.modrinth:create-tfmg:${deps.tfmgVersion}")
    // Chemica is optional: tools/build_chemica_compat.py reads its recipes from this jar, and -Pchemica runs it alongside.
    modCompileOnly("maven.modrinth:chemica:${deps.chemicaVersion}")
    if (providers.gradleProperty("chemica").isPresent) modLocalRuntime("maven.modrinth:chemica:${deps.chemicaVersion}")
    // Create ships these inside its jar, but a dev run does not unpack them.
    for (bundled in listOf("net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1",
            "dev.engine-room.flywheel:flywheel-neoforge-1.21.1:1.0.6", "com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")) {
        modCompileOnly(bundled)
        modLocalRuntime(bundled)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

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
        "neoforge_version" to deps.neoforgeVersion,
        "create_min" to deps.createMin,
        "tfmg_min" to deps.tfmgMin,
        "principles_min" to deps.principlesMin
    )

    props.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(tasks.named("build"))
    }
}

fun <T> optionalProp(property: String, block: (String) -> T?): T? =
    findProperty(property)?.toString()?.takeUnless { it.isBlank() }?.let(block)
