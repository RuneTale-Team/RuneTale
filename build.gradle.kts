import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.bundling.Zip

plugins {
    // Shadow makes “fat jars” (bundle your dependencies)
    id("com.gradleup.shadow") version "9.3.1" apply false
    // java apply false
    // `java-library` apply false
}

val vineflower by configurations.creating

val junitBomVersion = "5.14.3"
val mockitoVersion = "5.21.0"
val assertjVersion = "3.27.7"

dependencies {
    vineflower("org.vineflower:vineflower:1.11.2")
}

val runDir = layout.projectDirectory.dir("run")
val serverDir = layout.projectDirectory.dir("server")
val modsDir = serverDir.dir("mods")
val hytaleServerJar = serverDir.file("HytaleServer.jar").asFile
val decompileOutDir = layout.buildDirectory.dir("decompiled/hytale-server")
val unpackOutDir = layout.buildDirectory.dir("unpacked/hytale-server")
val filteredJar = layout.buildDirectory.file("tmp/HytaleServer-com-hypixel.jar")

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

        val sourceSets = extensions.getByType<SourceSetContainer>()
        tasks.named<Test>("test") {
            useJUnitPlatform {
                excludeTags("contract")
            }
        }

        tasks.register<Test>("contractTest") {
            group = "verification"
            description = "Runs contract-tagged tests."
            testClassesDirs = sourceSets.named("test").get().output.classesDirs
            classpath = sourceSets.named("test").get().runtimeClasspath
            shouldRunAfter(tasks.named("test"))
            useJUnitPlatform {
                includeTags("contract")
            }
        }

        tasks.named("check") {
            dependsOn("contractTest")
        }
    }
}

// Hytale plugin modules convention:
configure(subprojects.filter { it.path.startsWith(":plugins:") }) {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        "compileOnly"("com.hypixel.hytale:Server:+")
        "testCompileOnly"("com.hypixel.hytale:Server:+")
        "testRuntimeOnly"("com.hypixel.hytale:Server:+")
        "testImplementation"(project(":platform:testing-core"))
        "testImplementation"(project(":platform:testing-ecs"))
        "testImplementation"(project(":platform:testing-junit"))

        "testImplementation"(platform("org.junit:junit-bom:$junitBomVersion"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.mockito:mockito-core:$mockitoVersion")
        "testImplementation"("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        "testImplementation"("org.assertj:assertj-core:$assertjVersion")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }


    tasks.withType<Test>().configureEach {
        systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")
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

// Shared conventions for testing framework modules (":platform:testing-*")
configure(subprojects.filter { it.path.startsWith(":platform:testing-") }) {
    plugins.withType<JavaPlugin> {
        dependencies {
            "testImplementation"(platform("org.junit:junit-bom:$junitBomVersion"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testImplementation"("org.mockito:mockito-core:$mockitoVersion")
            "testImplementation"("org.mockito:mockito-junit-jupiter:$mockitoVersion")
            "testImplementation"("org.assertj:assertj-core:$assertjVersion")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

// All plugin subprojects (":plugins:*")
val pluginProjects = subprojects.filter { it.path.startsWith(":plugins:") }
val testingProjects = subprojects.filter { it.path.startsWith(":platform:testing-") }

tasks.register("unitTest") {
    group = "verification"
    description = "Runs unit tests (excludes contract-tagged tests)."
    dependsOn((pluginProjects + testingProjects).map { "${it.path}:test" })
}

tasks.register("contractTest") {
    group = "verification"
    description = "Runs contract-tagged tests across plugins and shared testing modules."
    dependsOn((pluginProjects + testingProjects).map { "${it.path}:contractTest" })
}

tasks.register("verifyTests") {
    group = "verification"
    description = "Runs unit and contract test suites."
    dependsOn("unitTest", "contractTest")
}

tasks.register<Delete>("cleanDeployedPlugins") {
    // Only delete jars that match your plugin project names, e.g. "skills-*.jar"
    val patterns = pluginProjects.map { "${it.name}-*.jar" }

    delete(
        fileTree(modsDir.asFile).apply {
            patterns.forEach { include(it) }
        }
    )

    doLast {
        println("Cleaned deployed plugin jars from: ${modsDir.asFile}")
    }
}

tasks.register<Delete>("cleanDeployedPluginBundles") {
    delete(
        fileTree(modsDir.asFile).apply {
            include("${rootProject.name}-plugin-jars-*.zip")
            include("*-plugin-jars-*.zip")
            include("${rootProject.name}-mods-bundle-*.zip")
            include("*-mods-bundle-*.zip")
        }
    )

    doLast {
        println("Cleaned deployed plugin bundle zips from: ${modsDir.asFile}")
    }
}

val skillsConfigResourceDir = layout.projectDirectory.dir("plugins/skills/src/main/resources/Skills")
val skillsConfigRunDir = layout.projectDirectory.dir("server/mods/runetale/config/skills")

tasks.register<Sync>("deploySkillsConfigToRun") {
    from(skillsConfigResourceDir)
    into(skillsConfigRunDir)

    doLast {
        println("Synced skills config to ${skillsConfigRunDir.asFile}")
    }
}

tasks.register<Zip>("bundlePluginJars") {
    dependsOn(pluginProjects.map { it.tasks.named("shadowJar") })
    dependsOn(pluginProjects.map { it.tasks.named("jar") })

    archiveBaseName.set("${rootProject.name}-plugin-jars")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(pluginProjects.map { project ->
        project.tasks.named<Jar>("shadowJar").flatMap { it.archiveFile }
    })
}

tasks.register<Zip>("bundleModsRelease") {
    dependsOn(pluginProjects.map { it.tasks.named("shadowJar") })
    dependsOn(pluginProjects.map { it.tasks.named("jar") })

    archiveBaseName.set("${rootProject.name}-mods-bundle")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(pluginProjects.map { project ->
        project.tasks.named<Jar>("shadowJar").flatMap { it.archiveFile }
    })

    from(skillsConfigResourceDir) {
        into("runetale/config/skills")
    }
}

tasks.register("deployPluginsToRun") {
    dependsOn("cleanDeployedPlugins")
    dependsOn("cleanDeployedPluginBundles")
    dependsOn(pluginProjects.map { it.tasks.named("shadowJar") })
    dependsOn("deploySkillsConfigToRun")

    doLast {
        copy {
            into(modsDir)
            from(pluginProjects.map { project ->
                project.tasks.named<Jar>("shadowJar").flatMap { it.archiveFile }
            })
        }
        println("Copied plugin jars to ${modsDir.asFile}")
    }
}

tasks.register<Jar>("makeFilteredHytaleJar") {
    inputs.file(hytaleServerJar)
    archiveFileName.set(filteredJar.get().asFile.name)
    destinationDirectory.set(filteredJar.get().asFile.parentFile)

    from(zipTree(hytaleServerJar)) {
        include("com/hypixel/**")
    }
}

tasks.register<Exec>("decompileHytaleServerJar") {
    dependsOn("makeFilteredHytaleJar")

    doFirst {
        val vineflowerJar = configurations.getByName("vineflower").singleFile
        val outPath = layout.buildDirectory.dir("decompiled/hytale-server").get().asFile.absolutePath
        val inJar = filteredJar.get().asFile.absolutePath

        commandLine(
            "java", "-jar", vineflowerJar.absolutePath,
            "--folder",
            inJar,
            outPath
        )
    }
}

tasks.register<Delete>("cleanDecompiledHytaleServerJar") {
    delete(decompileOutDir)
}
