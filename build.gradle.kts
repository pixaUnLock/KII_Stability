plugins {
    kotlin("jvm") version "1.8.10" apply false
    id("org.jetbrains.compose") version "1.4.0" apply false
}

group = "me.samol"
version = "1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
}