plugins {
    alias(libs.plugins.galva.android.feature)
}

android {
    namespace = "com.abrarshakhi.galva.feature.secrets"
}

dependencies {
    implementation(projects.core.vault)
    implementation(libs.media3.exoplayer)
}
