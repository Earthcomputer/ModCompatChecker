plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":root"))
    implementation(libs.bundles.asm)
    implementation(libs.gson)
    compileOnly(libs.jetbrainsAnnotations)
}
