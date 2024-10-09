// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Adiciona a dependência para o plugin do Google Services
    id("com.google.gms.google-services") version "4.4.2" apply false
}
