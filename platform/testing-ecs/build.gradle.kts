plugins {
    `java-library`
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:+")
    testCompileOnly("com.hypixel.hytale:Server:+")
    testRuntimeOnly("com.hypixel.hytale:Server:+")
    testImplementation(project(":platform:testing-junit"))
    api(project(":platform:testing-core"))
}
