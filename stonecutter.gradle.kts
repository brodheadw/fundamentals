plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-neoforge" /* [SC] DO NOT EDIT */

val buildAll = tasks.register("buildAll") {
    group = "project"
    description = "Builds every registered version × loader target."
}

stonecutter.tasks {
    val builds = named("build") { true }
    buildAll.configure { dependsOn(builds.map { it.values }) }
}
