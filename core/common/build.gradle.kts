plugins {
    alias(libs.plugins.galva.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)
}
