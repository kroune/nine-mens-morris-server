plugins {
    alias(libs.plugins.kotlin.jvm) apply true
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka) apply false
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xnon-local-break-continue")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi")
    }
}
