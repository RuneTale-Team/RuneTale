plugins {
    // Shadow makes “fat jars” (bundle your dependencies)
    id("com.gradleup.shadow") version "9.3.1" apply false
    // java apply false
    // `java-library` apply false
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()

        // Official Hytale maven repos (release + pre-release)
        maven("https://maven.hytale.com/release")
    }
}

subprojects {
    // Use Java toolchains everywhere (Java 25)
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}

// Hytale plugin modules convention:
configure(subprojects.filter { it.path.startsWith(":plugins:") }) {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        "compileOnly"("com.hypixel.hytale:Server:+")

        "testImplementation"(platform("org.junit:junit-bom:5.10.2"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
    }


    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // Produce a single distributable jar per plugin (no “-all” classifier)
    tasks.named<Jar>("shadowJar") {
        archiveClassifier.set("")
    }

    // Convenience: `./gradlew :plugins:skills:build` produces the plugin jar
    tasks.named("build") {
        dependsOn("shadowJar")
    }
}

// in root build.gradle.kts

val runDir = layout.projectDirectory.dir("run")
val modsDir = layout.projectDirectory.dir("server/mods")

tasks.register("deployPluginsToRun") {
    // build all plugin jars first
    dependsOn(subprojects.filter { it.path.startsWith(":plugins:") }.map { it.tasks.named("shadowJar") })

    doLast {
        copy {
            into(modsDir)
            from(subprojects.filter { it.path.startsWith(":plugins:") }.map { p ->
                p.layout.buildDirectory.dir("libs")
            })
            include("*.jar")
        }
        println("Copied plugin jars to ${modsDir.asFile}")
    }
}
