plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.manu.proyectogrupal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manu.proyectogrupal"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding=true
    }
}

dependencies {
    implementation(libs.firebase.database.ktx) // Asumiendo que libs.firebase.database.ktx está bien definido
    implementation(platform(libs.firebase.bom)) // BoM para manejar versiones de Firebase
    implementation(libs.firebase.auth)         // Asumiendo que libs.firebase.auth está bien definido y lo necesitas

    // Dependencias AndroidX y Material (Asumiendo que los alias libs... están bien definidos)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- CORRECCIÓN ZEGO ---
    // Comentamos/eliminamos las referencias libs antiguas o potencialmente incorrectas
    // implementation(libs.zego.uikit.prebuilt.call.android.v392)
    // implementation(libs.zegoCloud)

    // Añadimos la dependencia estándar de Zego UI Kit Prebuilt Call directamente
    // Puedes cambiar la versión '2.6.0' por la más reciente recomendada por ZegoCloud si lo deseas
    implementation(libs.zego.uikit.prebuilt.call.android.v392)
    // --- FIN CORRECCIÓN ZEGO ---


    // Play Services Base (Solo necesitas declararla una vez)
    implementation(libs.play.services.base)
    implementation(libs.androidx.espresso.core)
    implementation(libs.play.services.base)


    // Dependencias de Test (Asumiendo que los alias libs... están bien definidos)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core) // Usar androidTestImplementation para Espresso
}