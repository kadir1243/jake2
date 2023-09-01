import org.gradle.jvm.tasks.Jar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    java
}

group = "org.bytonic"
version = "1.1.2"

base {
    archivesName.set("jake2")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

tasks.withType<JavaCompile> {
    options.isDeprecation = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val lwjgl = "2.9.3"

dependencies {
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Client
    implementation("org.lwjgl.lwjgl:lwjgl:${lwjgl}")
    runtimeOnly("org.lwjgl.lwjgl:lwjgl-platform:${lwjgl}")
    implementation("org.lwjgl.lwjgl:lwjgl_util:${lwjgl}")
    implementation("javazoom:jlayer:1.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.register<Jar>("jarCommon") {
    dependsOn(JavaPlugin.JAR_TASK_NAME)
}

tasks.register<Jar>("jarClient") {
    this.dependsOn("jarCommon")
    this.manifest.attributes["Main-Class"] = "jake2.fullgame.Jake2"
    this.archiveClassifier.set("client")
}

tasks.register<Jar>("jarServer") {
    this.dependsOn("jarCommon")
    this.manifest.attributes["Main-Class"] = "jake2.dedicated.Jake2Dedicated"
    this.archiveClassifier.set("server")
}
