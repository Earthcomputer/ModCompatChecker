plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    implementation(libs.junitPlatform)
    implementation(project(":root"))
}
