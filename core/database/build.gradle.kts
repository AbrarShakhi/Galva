plugins {
    alias(libs.plugins.galva.android.library)
    alias(libs.plugins.galva.android.room)
}

android {
    namespace = "com.abrarshakhi.galva.core.database"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.room.runtime)

    implementation(libs.koin.android)
}
