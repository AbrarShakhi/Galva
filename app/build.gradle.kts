plugins {
    alias(libs.plugins.galva.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.abrarshakhi.galva"

    defaultConfig {
        applicationId = "com.abrarshakhi.galva"
        versionCode = 2
        versionName = "1.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.mediastore)
    implementation(projects.core.vault)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)

    implementation(projects.feature.gallery)
    implementation(projects.feature.albums)
    implementation(projects.feature.search)
    implementation(projects.feature.viewer)
    implementation(projects.feature.secrets)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
