import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

// CÓDIGO KOTLIN PARA CARREGAR AS PROPRIEDADES
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties() // Agora 'Properties' é reconhecido por causa do import
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    // CRIAR A CONFIGURAÇÃO DE ASSINATURA
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    namespace = "com.example.adotejr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.adotejr"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // CONFIGURAR O BUILD TYPE 'release'
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Associa a configuração de assinatura ao build type
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // -- Dependencias Firebase --
    implementation(platform("com.google.firebase:firebase-bom:33.9.0")) // Firebase BoM
    implementation("com.google.firebase:firebase-analytics") // Analytics
    implementation("com.google.firebase:firebase-auth") // Autenticacao
    implementation("com.google.firebase:firebase-firestore") // Banco de dados
    implementation("com.google.firebase:firebase-storage") // Armazenamento de Imagens
    // -- Fim Dependencias Firebase --

    implementation("com.google.android.material:material:1.3.0")

    // Picasso
    implementation("com.squareup.picasso:picasso:2.8")

    // Excel
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // WorkManager em Kotlin
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Para a biblioteca de gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Para o ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // Versão pode variar
    implementation("androidx.fragment:fragment-ktx:1.6.1") // Ajuda a conectar o Fragment ao ViewModel

}