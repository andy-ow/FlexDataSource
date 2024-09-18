import java.io.FileInputStream
import java.util.Properties

val artifactId = "flexds-core"
val groupId = "com.ajoinfinity.flexds"
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
                from(components["release"]) // For Kotlin/Java library
                groupId = groupId
                artifactId = artifactId
                version = android.defaultConfig.versionName
            }
        }
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}