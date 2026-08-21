plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "Foox.HoneyWasp"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Needed for MinnDevelopment Discord Webhooks
}


dependencies {
    implementation("net.dv8tion:JDA:6.4.1")
	implementation("org.json:json:20240303")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("org.bytedeco:javacv:1.5.12")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.4")
    implementation(platform("org.junit:junit-bom:5.10.0"))
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.junit.platform:junit-platform-launcher")
    implementation("org.jline:jline:3.30.6")
}

application {
    
    mainClass = "main.HoneyWasp"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("HoneyWasp")
        archiveClassifier.set("") // removes the -all suffix

        // exclude FFmpeg folder from being bundled into the jar
        exclude("ffmpeg/**")

        // Force output to IntelliJ artifact folder
        destinationDirectory.set(file("C:/Users/lande/Desktop/HoneyWasp"))
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
