plugins {
    alias(libs.plugins.galva.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.core)
}
