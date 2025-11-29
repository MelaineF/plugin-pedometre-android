plugins {
    id("com.android.library")
}

android {
    namespace = "com.favennec.libpodometreapi" // L'identifiant de votre bibliothèque
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        // Pas de 'targetSdk' ou 'applicationId' ici, c'est une bibliothèque
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Important pour ne pas supprimer de code utile
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
}

dependencies {
    // Gardez uniquement les dépendances ABSOLUMENT nécessaires
    // Pour l'instant, vous n'en avez pas besoin, ce qui est parfait.
}
