plugins {
    alias(libs.plugins.galva.android.library)
}

android {
    namespace = "com.abrarshakhi.galva.core.mediastore"
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}
