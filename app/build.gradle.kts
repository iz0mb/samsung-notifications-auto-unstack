val releaseVersionCode = (
    System.getenv("VERSION_CODE")
        ?: project.findProperty("VERSION_CODE")?.toString()
    )?.toIntOrNull() ?: 1

val releaseVersionName = System.getenv("VERSION_NAME")
    ?: project.findProperty("VERSION_NAME")?.toString()
    ?: "1.0"

val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
    ?: project.findProperty("KEYSTORE_PATH")?.toString()
val releaseKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
val releaseKeyAlias = System.getenv("KEY_ALIAS")
    ?: project.findProperty("KEY_ALIAS")?.toString()
    ?: "auto-unstack"
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
    ?: project.findProperty("KEY_PASSWORD")?.toString()
val releaseKeystoreFile = releaseKeystorePath?.takeIf { it.isNotBlank() }?.let { file(it) }
val hasReleaseSigning = releaseKeystoreFile?.isFile == true &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

plugins {
    id("com.android.application") version "8.4.1"
    id("org.jetbrains.kotlin.android") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.autounstack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autounstack.app"
        minSdk = 29
        targetSdk = 35
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    testImplementation(kotlin("test"))
}
