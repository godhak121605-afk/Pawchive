plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.all.pawchive"
    compileSdk = 36

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.all.pawchive"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.6.1"

        manifestPlaceholders += mapOf(
            "extensionName" to "Tachiyomi: Pawchive",
            "extensionClass" to "eu.kanade.tachiyomi.extension.all.pawchive.source.PawchiveSource",
        )
    }

    signingConfigs {
        create("release") {
            val keyStorePath = providers.environmentVariable("PAWCHIVE_KEYSTORE_PATH").orNull
            if (keyStorePath != null) {
                storeFile = file(keyStorePath)
                storePassword = providers.environmentVariable("PAWCHIVE_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("PAWCHIVE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("PAWCHIVE_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["extensionName"] = "[Debug] Tachiyomi: Pawchive"
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (providers.environmentVariable("PAWCHIVE_KEYSTORE_PATH").isPresent) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = false
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
        )
    }
}

dependencies {
    compileOnly("com.github.keiyoushi:extensions-lib:6e0c96cea8")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
