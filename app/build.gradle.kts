import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.knightscope.ksc842"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envProps = Properties().apply {
            val envFile = rootProject.file(".env")
            if (envFile.exists()) {
                envFile.bufferedReader(Charsets.UTF_8).use { load(it) }
            }
        }
        fun optionalSecret(key: String): String {
            val raw = (envProps.getProperty(key) ?: System.getenv(key) ?: "")
                .trim()
                .removeSurrounding("\"")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            return "\"$raw\""
        }
        buildConfigField("String", "FORMSPREE_ENDPOINT", optionalSecret("FORMSPREE_ENDPOINT"))
        buildConfigField("String", "EMAILJS_SERVICE_ID", optionalSecret("EMAILJS_SERVICE_ID"))
        buildConfigField("String", "EMAILJS_TEMPLATE_ID", optionalSecret("EMAILJS_TEMPLATE_ID"))
        buildConfigField("String", "EMAILJS_PUBLIC_KEY", optionalSecret("EMAILJS_PUBLIC_KEY"))
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Optional .env values are read safely in defaultConfig above with empty defaults

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    // implementation(libs.kotlinx.coroutines.play.services)
    // implementation(libs.coil.compose)

    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.firestore)
    // implementation(libs.firebase.auth)
    // implementation(libs.androidx.credentials)
    // implementation(libs.androidx.credentials.play.services)
    // implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<Test> {
    failFast = true
    timeout.set(Duration.ofMinutes(2))
    testLogging {
        events("passed", "failed", "skipped")
    }
}
