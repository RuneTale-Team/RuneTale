dependencies {
    compileOnly(project(":plugins:skills"))
    testImplementation(project(":plugins:skills"))
}

tasks.named("compileJava") {
    dependsOn(":plugins:skills:shadowJar")
}

tasks.named("compileTestJava") {
    dependsOn(":plugins:skills:shadowJar")
}
