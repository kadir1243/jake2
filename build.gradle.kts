plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
}

allprojects {
    group = "org.bytonic"
    version = "1.1.2"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

    tasks.withType<JavaCompile> {
        options.isDeprecation = true
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }
}

