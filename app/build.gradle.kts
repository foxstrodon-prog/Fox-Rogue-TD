import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Global-leaderboard backend settings (Firestore REST). Read from environment variables first, then
// from the git-ignored local.properties. When blank the game runs with local-only leaderboards.
val leaderboardProps = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
fun leaderboardSetting(name: String, default: String = ""): String =
  (System.getenv(name) ?: leaderboardProps.getProperty(name) ?: default).trim()

fun findGoogleServicesApiKey(): String {
  val gsFile = project.file("google-services.json")
  if (gsFile.exists()) {
    try {
      val text = gsFile.readText()
      val regex = """"current_key":\s*"([^"]+)"""".toRegex()
      val match = regex.find(text)
      if (match != null) return match.groupValues[1]
    } catch (_: Exception) {}
  }
  return ""
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.roguetd.xtkp"
    minSdk = 24
    targetSdk = 36
    versionCode = 32
    versionName = "0.0.1.011"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "LEADERBOARD_PROJECT_ID", "\"${leaderboardSetting("LEADERBOARD_PROJECT_ID", "roguetddualfront")}\"")
    buildConfigField("String", "LEADERBOARD_API_KEY", "\"${leaderboardSetting("LEADERBOARD_API_KEY", findGoogleServicesApiKey())}\"")
    buildConfigField("String", "LEADERBOARD_COLLECTION", "\"${leaderboardSetting("LEADERBOARD_COLLECTION", "leaderboards")}\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      if (keystorePath != null && file(keystorePath).exists()) {
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD") ?: ""
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
      }
    }
    create("debugConfig") {
      val dbgKeystore = file("${rootDir}/debug.keystore")
      if (!dbgKeystore.exists()) {
        val b64 = "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"
        dbgKeystore.writeBytes(Base64.getDecoder().decode(b64))
      }
      storeFile = dbgKeystore
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH")
      if (keystorePath != null && file(keystorePath).exists()) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.config)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.work.testing)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
