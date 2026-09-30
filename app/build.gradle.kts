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
    // The public identity of the app. Chosen once, before the first store release, because a store
    // listing can never change it later. It is derived from our own domain (webnovo.ir), so it also
    // cannot clash with somebody else's app - which is exactly what caused "package conflicts" for
    // users who happened to have another build under the old template id.
    applicationId = "ir.webnovo.novo"
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
        // Iranian stores (Cafe Bazaar / Myket) ask for the classic v1 JAR signature as well.
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
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

  // Two public identities:
  //  * standard -> ir.webnovo.novo ............ the app that goes to the stores / public links
  //  * legacy   -> com.aistudio.avamusic.player.canary ... the id the first testers already have,
  //                kept so their phones keep receiving real in-place updates (Android refuses to
  //                install a build whose package id OR signing key changed).
  flavorDimensions += "channel"
  productFlavors {
    create("standard") {
      dimension = "channel"
      // Store builds are signed with the private upload key when it is configured
      // (see keystore/README.md + the Make Upload Keystore workflow). While it is not set up we
      // fall back to the project's stable debug key, so builds stay installable and updatable.
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
    create("legacy") {
      dimension = "channel"
      applicationId = "com.aistudio.avamusic.player.canary"
      // Pinned to the debug key on purpose: this is the channel the first testers already have,
      // and switching its key would make their in-place updates fail with "package conflicts".
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // No signing config here on purpose: each flavour below decides which key it is signed with.
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
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
