plugins {
    kotlin("jvm") version "1.5.0" apply false
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}