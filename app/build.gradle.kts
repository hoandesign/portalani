import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

val localProps =
    Properties().apply {
      val file = rootProject.file("local.properties")
      if (file.exists()) file.inputStream().use { load(it) }
    }

val anilistClientId = localProps.getProperty("ANILIST_CLIENT_ID", "")
val anilistClientSecret = localProps.getProperty("ANILIST_CLIENT_SECRET", "")

android {
  namespace = "com.portal.portalani"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.portal.portalani"
    minSdk = 28
    targetSdk = 29
    versionCode = 28
    versionName = "0.7.3"

    buildConfigField("String", "ANILIST_CLIENT_ID", "\"$anilistClientId\"")
    buildConfigField("String", "ANILIST_CLIENT_SECRET", "\"$anilistClientSecret\"")
    buildConfigField("String", "ANILIST_REDIRECT_URI", "\"portalani://callback\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.browser)
  implementation(libs.okhttp)
  implementation(libs.coil.compose)
  testImplementation(libs.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
