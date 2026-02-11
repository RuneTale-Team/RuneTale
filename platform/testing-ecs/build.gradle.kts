plugins {
    `java-library`
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:+")
    api(project(":platform:testing-core"))
}
