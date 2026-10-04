package com.abrarshakhi.galva.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.compose = true
    dependencies {
        val bom = libs.library("androidx-compose-bom")
        "implementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
        "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
    }
}
