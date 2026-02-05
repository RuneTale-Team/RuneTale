plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


application {
    // Define the main class for the application.
    mainClass = "org.runetale.skills."
}