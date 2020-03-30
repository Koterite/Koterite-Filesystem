plugins {
    kotlin("multiplatform") version "1.3.71"
}

group = "br.com.gamemods.koterite"
version = "1.0.0-SNAPSHOT"

repositories {
    jcenter()
}

val ktor_version: String by project
val mingwPath = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")
println("!!!!!!!!!!!!!!!!!!!!!$mingwPath!!!!!!!!!!!!!!!!!!!!!!!!")

kotlin {
    jvm()
    mingwX64("windows") {
        binaries {
            sharedLib {
                baseName = "koterite-filesystem"
                linkerOpts("-L${mingwPath.resolve("lib")}")
            }
        }
    }
    //linuxX64()
    /* Targets configuration omitted.
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib-common"))
                api("io.ktor:ktor-client-core:$ktor_version")
                api("br.com.gamemods.koterite:koterite-annotations:1.0.0-SNAPSHOT")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib"))
                api("io.ktor:ktor-client:$ktor_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val windowsMain by getting {
            dependencies {
                api("io.ktor:ktor-client-curl:$ktor_version")
            }
        }

        all {
            languageSettings.apply {
                //useExperimentalAnnotation("kotlin.ExperimentalMultiplatform")
                //useExperimentalAnnotation("kotlin.RequiresOptIn")
                enableLanguageFeature("InlineClasses")
            }
        }
    }
}
