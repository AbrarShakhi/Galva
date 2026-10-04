plugins {
    alias(libs.plugins.galva.android.library.compose)
}

android {
    namespace = "com.abrarshakhi.galva.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.graphics.shapes)

    implementation(libs.androidx.core.ktx)
    implementation(libs.lottie.compose)
    implementation(libs.material.kolor)
}
