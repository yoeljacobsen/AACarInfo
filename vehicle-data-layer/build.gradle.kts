
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.aacarinfo.vehicle.data.layer"
    compileSdk = 35

    defaultConfig {
        minSdk = rootProject.extra["min_sdk_version"] as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":common-data"))

    implementation("androidx.car.app:app:" + rootProject.extra["androidx_car_app_version"])
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:" + rootProject.extra["kotlin_coroutines_version"])
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:" + rootProject.extra["kotlin_coroutines_version"])

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
