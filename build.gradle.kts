plugins {
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "9.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
    id("org.graalvm.buildtools.native") version "1.1.8"
    application
}

group = "snkt.org"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("snkt.org.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
            buildArgs.add("--static")
        }
    }
}


dependencies {
    // Source: https://mvnrepository.com/artifact/com.unboundid/unboundid-ldapsdk
    implementation("com.unboundid:unboundid-ldapsdk:7.0.5")
    // Source: https://mvnrepository.com/artifact/io.github.oshai/kotlin-logging-jvm
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    // Source: https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.18")
    // Source: https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.38")
    // Source: https://mvnrepository.com/artifact/com.cronutils/cron-utils
    implementation("com.cronutils:cron-utils:9.2.1")
    // Source: https://mvnrepository.com/artifact/net.peanuuutz.tomlkt/tomlkt
    implementation("net.peanuuutz.tomlkt:tomlkt:0.5.0")
    // Source: https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-core
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    // Source: https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

    // Source: https://mvnrepository.com/artifact/io.github.serpro69/kotlin-faker
    testImplementation("io.github.serpro69:kotlin-faker:2.0.0-rc.13")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    // Source: https://mvnrepository.com/artifact/io.strikt/strikt-core
    implementation("io.strikt:strikt-core:0.35.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}