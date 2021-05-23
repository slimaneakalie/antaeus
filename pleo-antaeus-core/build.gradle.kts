plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
    api(project(":pleo-antaeus-models"))
}