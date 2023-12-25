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

    testImplementation(platform(libs.junit))
    testImplementation(libs.junitPlatform)
    testImplementation(project(":testCommon"))
}

tasks.test {
    dependsOn(":testFabricMod:jar")
    dependsOn(":testNewLib:jar")
    useJUnitPlatform()

    systemProperties(
        "testMod.jar" to project(":testFabricMod").tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath,
        "testNewLib.jar" to project(":testNewLib").tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath,
    )
}
