/**
 * NOTE: This is entirely optional and basics can be done in `settings.gradle.kts`
 */

import org.gradle.jvm.toolchain.JavaLanguageVersion

repositories {
    // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
}

dependencies {
    // Any external dependency you also want to include
}

subprojects {
    plugins.withId("java") {
        extensions.configure<org.gradle.api.plugins.JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}
