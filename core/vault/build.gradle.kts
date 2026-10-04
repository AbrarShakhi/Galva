plugins {
    alias(libs.plugins.galva.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.abrarshakhi.galva.core.vault"
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.common)
    implementation(projects.core.mediastore)
    implementation(libs.androidx.core.ktx)
    implementation(libs.tink.android)
    implementation(libs.argon2kt)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.coil.core)
    implementation(libs.media3.datasource)
    implementation(libs.koin.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
