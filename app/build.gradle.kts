plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Plugin para o Firebase
}

android {
    namespace = "com.example.gerenciadordeprodutos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gerenciadordeprodutos"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    // Dependências padrão do Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.eazegraph)
    implementation (libs.library)
    // Firebase BoM para controle de versões centralizado
    implementation(platform(libs.firebase.bom))
    implementation (libs.androidx.cardview)
    // Dependências do Firebase
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth)
    implementation (libs.material)
    implementation (libs.core.ktx.v1100)
    implementation (libs.itextpdf.itext7.core) // Versão do iText
    implementation(libs.play.services.auth)
    implementation (libs.material.v190)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    // Dependências para testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.squareup.picasso)
}
