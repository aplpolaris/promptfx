plugins {
    id("java-library")
}

group = "com.googlecode.blaisemath"
version = "0.10.2"

val appName = "promptfx"
val appVersion = "0.10.2"

repositories {
    mavenCentral()
}

// Configuration to extract just the jar from the dependency
val appJar by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    appJar("$group:$appName:$appVersion:jar-with-dependencies@jar")
}

val platformsWithExts = mapOf(
    "windows" to "bat",
    "macos" to "command",
    "linux" to "sh"
)

// Register per-platform prepareDist tasks
platformsWithExts.forEach { (platform, ext) ->
    tasks.register<Copy>("prepareDist${platform.capitalize()}") {
        dependsOn(appJar)

        destinationDir = layout.buildDirectory.dir("dist/$platform").get().asFile

        doFirst {
            if (destinationDir.exists()) {
                destinationDir.deleteRecursively()
            }
        }
        from("src/main/launchers/run-$platform.$ext") {
            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to mapOf("version" to version))
            filteringCharset = "UTF-8"
        }
        from("src/main/dist")
        from(appJar) {
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