import java.io.FileInputStream
import java.util.Properties

val artifactId = "flexds-core"
val groupId = "com.ajoinfinity.flexds"
val myversion = "0.0.37"

group = groupId

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `maven-publish`
}

val githubProperties = Properties()
githubProperties.load(FileInputStream("/home/andrzej/programowanie/android/github.properties"))
project.afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/andy-ow/FlexDataSource")
                credentials {
                    username = githubProperties["gpr.user"] as String? ?: System.getenv("GPR_USER")
                    password =
                        githubProperties["gpr.key"] as String? ?: System.getenv("GPR_API_KEY")
                }
            }
        }
        publications {
            create<MavenPublication>("library") {
                from(components["java"]) // For Kotlin/Java library
                groupId = groupId
                artifactId = artifactId
                version = myversion
            }
        }
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}