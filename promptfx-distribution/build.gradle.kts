plugins {
    id("java-library")
}

group = "com.googlecode.blaisemath"
version = "0.10.2"

val appName = "promptfx"
val appVersion = "0.10.3-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configuration to extract just the jar from the dependency
val appJarLinux by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val appJarWindows by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val appJarMacos by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val appJarMacos64 by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    appJarWindows("$group:$appName:$appVersion:windows@jar")
    appJarMacos("$group:$appName:$appVersion:macos@jar")
    appJarMacos64("$group:$appName:$appVersion:mac64@jar")
    appJarLinux("$group:$appName:$appVersion:linux@jar")
}

val platformConfigs = mapOf(
    "windows" to appJarWindows,
    "macos" to appJarMacos,
    "mac64" to appJarMacos64,
    "linux" to appJarLinux
)
val platformsWithExts = mapOf(
    "windows" to "bat",
    "macos" to "command",
    "mac64" to "command",
    "linux" to "sh"
)

// Register per-platform prepareDist tasks
platformsWithExts.forEach { (platform, ext) ->
    val config = platformConfigs[platform]!!
    tasks.register<Copy>("prepareDist${platform.capitalize()}") {
        dependsOn(config)
        destinationDir = layout.buildDirectory.dir("dist/$platform").get().asFile
        doFirst { if (destinationDir.exists()) destinationDir.deleteRecursively() }
        from("src/main/launchers/run-$platform.$ext") {
            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to mapOf("version" to version))
            filteringCharset = "UTF-8"
        }
        from("src/main/dist")
        from(config.singleFile) {
            rename { "promptfx-$version.jar" }
        }
    }
}

// Combine all into one logical prepareDist task (optional)
tasks.register("prepareDist") {
    dependsOn(platformsWithExts.keys.map { "prepareDist${it.capitalize()}" })
}

// Zip tasks (unchanged)
platformsWithExts.keys.forEach { platform ->
    tasks.register<Zip>("zip${platform.capitalize()}") {
        dependsOn("prepareDist${platform.capitalize()}")
        from(layout.buildDirectory.dir("dist/$platform"))
        archiveFileName.set("$appName-$version-$platform.zip")
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    }
}

tasks.register("zipAll") {
    dependsOn(platformsWithExts.keys.map { "zip${it.capitalize()}" })
}