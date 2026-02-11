plugins {
    `java-library`
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:+")
    testCompileOnly("com.hypixel.hytale:Server:+")
    testRuntimeOnly("com.hypixel.hytale:Server:+")
    api(project(":platform:testing-core"))
}
