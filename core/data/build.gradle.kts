plugins {
    alias(libs.plugins.galva.android.library)
}

android {
    namespace = "com.abrarshakhi.galva.core.data"
}

abstract class BundleDocumentsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val documents: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun bundle() {
        val target = outputDirectory.get().asFile.resolve("documents")
        target.deleteRecursively()
        target.mkdirs()
        documents.forEach { document -> document.copyTo(target.resolve(document.name), overwrite = true) }
    }
}

val bundleDocuments = tasks.register<BundleDocumentsTask>("bundleDocuments") {
    documents.from(
        rootProject.layout.projectDirectory.files("ABOUT.md", "TERMS.md", "PRIVACY.md", "CREDITS.md"),
    )
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            bundleDocuments,
            BundleDocumentsTask::outputDirectory,
        )
    }
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.mediastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.koin.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
