import com.abrarshakhi.galva.buildlogic.GalvaSdk
import com.abrarshakhi.galva.buildlogic.configureKotlinAndroid
import com.abrarshakhi.galva.buildlogic.library
import com.abrarshakhi.galva.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = GalvaSdk.TARGET
            }

            dependencies {
                "testImplementation"(libs.library("junit"))
            }
        }
    }
}
