import com.abrarshakhi.galva.buildlogic.configureAndroidCompose
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "galva.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}
