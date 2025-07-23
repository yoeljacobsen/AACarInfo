
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

val min_sdk_version by extra(29)
val compile_sdk_version by extra(35)
val target_sdk_version by extra(35)
val androidx_car_app_version by extra("1.7.0")
val kotlin_coroutines_version by extra("1.7.3")
