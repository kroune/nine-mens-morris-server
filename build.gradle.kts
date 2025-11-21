plugins {
    alias(libs.plugins.kotlin.jvm) apply true
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka) apply false
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xnon-local-break-continue")
    }
}
