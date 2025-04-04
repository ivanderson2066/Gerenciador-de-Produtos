plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Plugin para o Firebase
}

android {
    namespace = "br.com.NoxEstoque.brasil"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.NoxEstoque.brasil"
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
    // Habilitar o ViewBinding no Kotlin DSL
    viewBinding {
        enable = true
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

    // Bibliotecas adicionais
    implementation(libs.eazegraph) // Gráficos
    implementation(libs.library)   // Biblioteca customizada

    // Firebase BoM para controle de versões centralizado
    implementation(platform(libs.firebase.bom))

    // Dependências do Firebase
    implementation(libs.firebase.auth.ktx)       // Firebase Authentication
    implementation(libs.firebase.analytics.ktx)  // Firebase Analytics
    implementation(libs.firebase.firestore.ktx)  // Firestore
    implementation(libs.firebase.database.ktx)   // Firebase Database
    implementation(libs.firebase.storage.ktx)    // Firebase Storage

    // Outras dependências úteis
    implementation(libs.androidx.cardview)       // CardView para UI
    implementation(libs.itextpdf.itext7.core)    // iText PDF Library
    implementation(libs.play.services.auth)      // Google Play Services Auth
    implementation(libs.squareup.picasso)        // Picasso para carregar imagens
    implementation(libs.glide)                   // Glide para carregamento de imagens
    annotationProcessor(libs.compiler)           // Processador de anotações para Glide (se necessário)

    // Dependências para testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
