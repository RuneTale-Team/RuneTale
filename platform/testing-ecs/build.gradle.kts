plugins {
    `java-library`
}

val hytaleServerVersion = providers.gradleProperty("hytaleServerVersion").get()

dependencies {
    compileOnly("com.hypixel.hytale:Server:$hytaleServerVersion")
    testCompileOnly("com.hypixel.hytale:Server:$hytaleServerVersion")
    testRuntimeOnly("com.hypixel.hytale:Server:$hytaleServerVersion")
    testImplementation(project(":platform:testing-junit"))
    api(project(":platform:testing-core"))
}
