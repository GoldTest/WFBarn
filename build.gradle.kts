@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    id("com.android.application") version "8.2.0"
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

group = "com.wfbarn"
version = "0.2.1"

kotlin {
    androidTarget()
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                val ktorVersion = "2.3.7"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-auth:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.8.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
                implementation("io.ktor:ktor-client-android:2.3.7")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
            }
        }
    }
}

android {
    namespace = "com.wfbarn"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources", "src/desktopMain/resources")

    defaultConfig {
        applicationId = "com.wfbarn"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "0.2.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "WFBarn"
            packageVersion = "0.1.0"
            description = "WFBarn Money Management System"
            copyright = "© 2025 WFBarn"
            vendor = "WFBarn"

            windows {
                shortcut = true
                menu = true
                iconFile.set(project.file("src/desktopMain/resources/windows/WFBarn.ico"))
            }

            modules("java.sql", "java.desktop", "jdk.unsupported", "java.instrument")
            includeAllModules = false
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
            version.set("7.4.0")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

tasks.register<Zip>("packagePortable") {
    group = "compose desktop"
    description = "Packages the application as a portable zip file."
    dependsOn("createDistributable")
    from("build/compose/binaries/main/app")
    archiveFileName.set("WFBarn-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/zip"))
}

tasks.register<Zip>("packageReleasePortable") {
    group = "compose desktop"
    description = "Packages the application as a release portable zip file."
    dependsOn("createReleaseDistributable")
    from("build/compose/binaries/main-release/app")
    archiveFileName.set("WFBarn-portable-release.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/zip"))
}
