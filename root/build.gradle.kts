plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    implementation(libs.joptSimple)
    implementation(libs.bundles.asm)
    testImplementation(platform(libs.junit))
    testImplementation(libs.junitPlatform)
    testImplementation(project(":testCommon"))
}

tasks.test {
    dependsOn(":testMod:jar")
    dependsOn(":testNewLib:jar")
    useJUnitPlatform()

    systemProperties(
        "testMod.jar" to project(":testMod").tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath,
        "testNewLib.jar" to project(":testNewLib").tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath,
    )
}
