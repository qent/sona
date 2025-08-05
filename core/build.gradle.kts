plugins {
    alias(libs.plugins.kotlin)
}

group = rootProject.property("pluginGroup") as String
version = rootProject.property("pluginVersion") as String

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.kotlin)
    implementation(libs.langchain4j.anthropic)
    implementation(libs.langchain4j.mcp)
}
