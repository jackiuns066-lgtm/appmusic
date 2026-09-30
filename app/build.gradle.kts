plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

// Every CI build gets a higher versionCode, so the APK is always a genuine *update* that installs
// straight over the version already on the phone (same package id + same signing key + newer code).
// 1.1.0 -> 10100, then the GitHub run number is added (base + run).
val baseVersionCode = 10100
val ciRunNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull() ?: 0

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.avamusic.player"
    minSdk = 24
    targetSdk = 36
    versionCode = baseVersionCode + ciRunNumber
    versionName = "1.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // The debug keystore is committed on purpose: GitHub runners would otherwise generate a new
  // random debug key on every build, and Android refuses to install an APK over one that was
  // signed with a different key ("package conflicts with an existing package").
  signingConfigs {
    getByName("debug") {
      val debugKeystore = rootProject.file("keystore/novo-debug.p12")
      if (debugKeystore.exists()) {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
        storeType = "PKCS12"
      }
    }

    // Release/upload key: supplied through GitHub secrets (never committed).
    val uploadKeystore = rootProject.file("keystore/novo-upload.p12")
    val uploadPassword = providers.environmentVariable("NOVO_KEYSTORE_PASSWORD").orNull
    if (uploadKeystore.exists() && !uploadPassword.isNullOrBlank()) {
      create("release") {
        storeFile = uploadKeystore
        storePassword = uploadPassword
        keyAlias = providers.environmentVariable("NOVO_KEY_ALIAS").orNull ?: "novo-upload"
        keyPassword = providers.environmentVariable("NOVO_KEY_PASSWORD").orNull ?: uploadPassword
        storeType = "PKCS12"
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Falls back to the stable debug key when no upload key is configured, so the release APK
      // is still installable while the store keys are being set up.
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }

    // "canary" = the same app under a different package id (com.aistudio.avamusic.player.canary).
    // It exists so a build can ALWAYS be installed, even when a copy of the app is still hiding on
    // the phone (second space, secure folder, dual apps, work profile ...) and blocks the normal
    // package with "package conflicts with an existing package".
    create("canary") {
      initWith(getByName("release"))
      applicationIdSuffix = ".canary"
      isMinifyEnabled = false
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.media)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.runner)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
