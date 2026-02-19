plugins {
    `java-library`
}

dependencies {
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.14.3")
    api(project(":platform:testing-core"))
}
