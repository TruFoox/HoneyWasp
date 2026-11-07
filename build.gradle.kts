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
    implementation("net.dv8tion:JDA:6.7.0")
	implementation("org.json:json:20260814")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("tools.jackson.core:jackson-databind:3.2.4")
    implementation("org.jline:jline:4.5.0")
    implementation("org.eclipse.jetty:jetty-server:12.1.14")
    implementation("org.eclipse.jetty.websocket:jetty-websocket-jetty-server:12.1.14")
    implementation("com.microsoft.playwright:playwright:1.64.0")
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
