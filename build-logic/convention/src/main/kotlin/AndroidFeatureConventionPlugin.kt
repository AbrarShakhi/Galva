import com.abrarshakhi.galva.buildlogic.library
import com.abrarshakhi.galva.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "galva.android.library.compose")

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:data"))
                "implementation"(project(":core:model"))

                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("koin-androidx-compose"))

                "testImplementation"(libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
