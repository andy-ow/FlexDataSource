plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "1.9.25"
}

android {
    namespace = "com.ajoinfinity.flexdsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ajoinfinity.flexdsapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //implementation("com.github.andy-ow:FlexDataSource-flexds-core:0.0.8")
    implementation("com.ajoinfinity.flexds:flexds-core:0.0.8")
    implementation("com.ajoinfinity.flexds:flexds-fb-storage:0.0.5")
    //implementation(project(":flexds-core"))
    //implementation(project(":flexds-fb-realtime"))
    //implementation(project(":flexds-fb-storage"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":flexds-fb-storage"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}