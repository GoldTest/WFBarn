import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
}

group = "com.wfbarn"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/interactive-graphics")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "WFBarn"
            packageVersion = "0.0.1"
            description = "WFBarn Money Management System"
            copyright = "© 2025 WFBarn"
            vendor = "WFBarn"

            windows {
                shortcut = true
                menu = true
            }

            modules("java.sql", "java.desktop")
        }
    }
}
