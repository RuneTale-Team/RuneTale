rootProject.name = "org.runetale"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
    id("dev.scaffoldit") version "0.2.5"
}

common {

}

hytale {
    include("skills") {
        manifest {
            Group = "RuneTale"
            Name = "RuneTaleSkills"
            Main = "org.runetale.SkillsPlugin"
        }
    }
}