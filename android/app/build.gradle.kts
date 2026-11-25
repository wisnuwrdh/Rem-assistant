plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ecosystemku.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ecosystemku.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"

        val apiBaseUrl = (providers.gradleProperty("API_BASE_URL").orNull
            ?: System.getenv("API_BASE_URL")
            ?: "https://ais-pre-ofggbiqriitpg36wilx47j-898717817150.asia-east1.run.app")
            .trim()
            .ifEmpty { "https://ais-pre-ofggbiqriitpg36wilx47j-898717817150.asia-east1.run.app" }

        // URL backend server (/api/chat & /api/tts). Bisa di-override via Gradle property atau env API_BASE_URL di CI.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"$apiBaseUrl\""
        )
    }

    // Keystore tetap (di-commit) supaya signature APK sama di setiap build/CI,
    // sehingga update bisa menimpa versi lama tanpa uninstall.
    signingConfigs {
        create("fixed") {
            storeFile = file("rem-assistant.jks")
            storeType = "pkcs12"
            storePassword = "remassistant"
            keyAlias = "rem"
            keyPassword = "remassistant"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            signingConfig = signingConfigs.getByName("fixed")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // Pastikan file Live2D .moc3 & audio .mp3 tidak dikompresi ulang
        noCompress += listOf("moc3", "mp3", "json", "png")
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.webkit)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.play.services.auth)

    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
