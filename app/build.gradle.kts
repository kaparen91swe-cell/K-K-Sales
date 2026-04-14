import java.util.Properties
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.kksales"
    compileSdk = 35

    // Robust versionshantering
    val versionPropsFile = rootProject.file("version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        versionProps.load(versionPropsFile.inputStream())
    } else {
        versionProps.setProperty("VERSION_CODE", "1")
        versionProps.setProperty("VERSION_NAME", "1.0.1")
        versionProps.store(versionPropsFile.writer(), null)
    }

    var currentCode = versionProps.getProperty("VERSION_CODE").toInt()
    
    defaultConfig {
        applicationId = "com.example.kksales"
        minSdk = 26
        targetSdk = 35

        versionCode = currentCode
        versionName = "1.0.$currentCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")
        testHandleProfiling = true
        testFunctionalTest = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Automatisk versionshantering vid bygge - körs precis innan själva bygget
    gradle.taskGraph.whenReady {
        if (hasTask(":app:assembleDebug") || hasTask(":app:assembleRelease")) {
            val vFile = rootProject.file("version.properties")
            val vProps = Properties()
            if (vFile.exists()) {
                vProps.load(vFile.inputStream())
                val nextCode = vProps.getProperty("VERSION_CODE").toInt() + 1
                vProps.setProperty("VERSION_CODE", nextCode.toString())
                vProps.setProperty("VERSION_NAME", "1.0.$nextCode")
                vProps.store(vFile.writer(), null)
                
                // Uppdatera även version.json automatiskt för GitHub
                val versionJsonFile = rootProject.file("version.json")
                val apkUrl = "https://github.com/kaparen91swe-cell/K-K-Sales/releases/download/v1.0.$nextCode/app-debug.apk"
                versionJsonFile.writeText("{\n  \"versionCode\": $nextCode,\n  \"apkUrl\": \"$apkUrl\"\n}")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.play.services.mlkit.document.scanner)
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}
