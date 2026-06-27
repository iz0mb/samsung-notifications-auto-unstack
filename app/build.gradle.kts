val releaseVersionCode = (
    System.getenv("VERSION_CODE")
        ?: project.findProperty("VERSION_CODE")?.toString()
    )?.toIntOrNull() ?: 1

val releaseVersionName = System.getenv("VERSION_NAME")
    ?: project.findProperty("VERSION_NAME")?.toString()
    ?: "1.0"

fun readSigningValue(name: String): String? = (
    System.getenv(name)
        ?: project.findProperty(name)?.toString()
    )?.trim()?.takeIf { it.isNotEmpty() }

val releaseKeystorePath = readSigningValue("KEYSTORE_PATH")
val releaseKeystorePassword = readSigningValue("KEYSTORE_PASSWORD")
val releaseKeyAlias = readSigningValue("KEY_ALIAS") ?: "auto-unstack"
val releaseKeyPassword = readSigningValue("KEY_PASSWORD")
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
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
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
