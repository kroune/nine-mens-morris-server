plugins {
    alias(libs.plugins.kotlin.jvm) apply true
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xnon-local-break-continue")
        }
        @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
        abiValidation {
            // Use the set() function to ensure compatibility with older Gradle versions
            enabled.set(true)
        }
    }
}
