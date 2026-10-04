plugins {
    alias(libs.plugins.galva.android.library.compose)
}

android {
    namespace = "com.abrarshakhi.galva.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(projects.core.domain)
    implementation(projects.core.mediastore)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(libs.kotlinx.coroutines.test)
}
