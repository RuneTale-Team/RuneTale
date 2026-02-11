plugins {
    `java-library`
}

dependencies {
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.14.2")
    api(project(":platform:testing-core"))
}
