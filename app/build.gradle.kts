import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
}

// Firma fija: todas las versiones tienen que firmarse con la misma clave para poder
// instalarse encima de la anterior. En GitHub Actions la clave llega desde el secreto
// FIRMA_KEYSTORE_B64 (ver .github/workflows/build.yml); si no está, se usa la clave debug local.
val firmaFija = System.getenv("EPEN_FIRMA")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "ar.pariz.epen"
    compileSdk = 37

    defaultConfig {
        applicationId = "ar.pariz.epen"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "1.5"
    }

    signingConfigs {
        if (firmaFija != null) create("epen") {
            storeFile = firmaFija
            storePassword = System.getenv("EPEN_FIRMA_CLAVE") ?: "android"
            keyAlias = System.getenv("EPEN_FIRMA_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("EPEN_FIRMA_CLAVE") ?: "android"
        }
    }

    buildTypes {
        release {
            // R8 optimiza y achica el código de la app; las librerías de PDF quedan intactas
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("epen") ?: signingConfigs.getByName("debug")
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("androidx.test:core:1.7.0")
}
