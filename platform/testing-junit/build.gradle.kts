plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.junit.jupiter.api)
    api(project(":platform:testing-core"))
}
