import org.gradle.api.initialization.resolve.RepositoriesMode

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://maven.hytale.com/release")
    }
}

rootProject.name = "RuneTale"

include(":platform:testing-core")
include(":platform:testing-ecs")
include(":platform:testing-junit")
include(":plugins:skills")
include(":plugins:skills-api")
include(":plugins:skills-gathering")
include(":plugins:skills-crafting")
include(":plugins:skills-equipment")
include(":plugins:skills-combat")
include(":plugins:skills-actions")
include(":plugins:block-regeneration")
