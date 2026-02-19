dependencies {
    compileOnly(project(":plugins:skills-api"))
    testImplementation(project(":plugins:skills-api"))
}

tasks.named("compileJava") {
    dependsOn(":plugins:skills-api:shadowJar")
}

tasks.named("compileTestJava") {
    dependsOn(":plugins:skills-api:shadowJar")
}
