plugins {
    id("java")
}

group = "net.earthcomputer"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-analysis:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
