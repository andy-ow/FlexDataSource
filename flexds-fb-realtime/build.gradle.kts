import java.io.FileInputStream
import java.util.Properties

val artifactId = "flexds-fb-realtime"
val groupId = "com.ajoinfinity.flexds"
group = groupId
val myversion = "0.0.12"

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
                version = myversion
            }
        }
    }
}

android {
    namespace = "com.ajoinfinity.flexds"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":flexds-core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

