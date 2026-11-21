import java.time.LocalDate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.monager.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.monager.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "0.5.1"
        buildConfigField("String", "RELEASE_DATE", "\"${LocalDate.now()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { applicationIdSuffix = ".debug" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Keep only the languages you ship.
    androidResources { localeFilters += listOf("vi", "en") }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

// PERF_TOOLS (JankStats logging + demo-data seeding for benchmarks) is on for every build type except the real release.
androidComponents {
    onVariants { v ->
        v.buildConfigFields?.put("PERF_TOOLS", com.android.build.api.variant.BuildConfigField("boolean", (v.buildType != "release").toString(), null))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.metrics.performance) // used only behind BuildConfig.PERF_TOOLS; R8 strips it from release

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.room.testing)
}
