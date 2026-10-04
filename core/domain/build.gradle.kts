plugins {
    alias(libs.plugins.galva.android.library)
}

android {
    namespace = "com.abrarshakhi.galva.core.domain"
}

dependencies {
    api(projects.core.model)
    api(projects.core.data)
    api(projects.core.vault)

    implementation(libs.koin.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
