plugins {
    alias(libs.plugins.galva.android.feature)
}

android {
    namespace = "com.abrarshakhi.galva.feature.viewer"
}

dependencies {
    implementation(libs.media3.exoplayer)
}
