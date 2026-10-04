pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.architectury.dev")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    // Multi-version preprocessor/controller (https://stonecutter.kikugie.dev)
    id("dev.kikugie.stonecutter") version "0.7.5"
    // Auto-provisions the JDK 21 toolchain that Minecraft 1.21.x requires,
    // regardless of the JDK running Gradle.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        fun mc(mcVersion: String, loaders: Iterable<String>) {
            for (loader in loaders) {
                vers("$mcVersion-$loader", mcVersion)
            }
        }

        // ---------------------------------------------------------------
        // Supported (Minecraft version × loader) targets.
        // To add a new Minecraft version: add an `mc(...)` line here and
        // create a matching `versions/<mc>-<loader>/gradle.properties`.
        // See ROADMAP.md.
        // ---------------------------------------------------------------
        mc("1.21.1", listOf("fabric", "neoforge"))

        vcsVersion = "1.21.1-fabric"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "fundamentals"
