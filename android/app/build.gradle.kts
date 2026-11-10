plugins {
 id("com.android.application")
 kotlin("android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.instantroom"
 compileSdk = 37
 buildToolsVersion = "37.0.0"
 defaultConfig {
  applicationId = "com.instantroom"
  minSdk = 26
  targetSdk = 37
  versionCode = 5
  versionName = "0.5.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 signingConfigs.getByName("debug") {
  System.getenv("INSTANT_ROOM_DEBUG_KEYSTORE")?.let {storeFile=file(it)}
 }
 buildFeatures { compose = true; buildConfig = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
 sourceSets.getByName("test").resources.srcDir("schemas")
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlin { jvmToolchain(21); compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(project(":core"))
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.camera:camera-camera2:1.6.2")
 implementation("androidx.camera:camera-lifecycle:1.6.2")
 implementation("androidx.camera:camera-view:1.6.2")
 implementation("androidx.room:room-runtime:2.8.5")
 implementation("androidx.room:room-ktx:2.8.5")
 ksp("androidx.room:room-compiler:2.8.5")
 implementation("androidx.exifinterface:exifinterface:1.4.2")
 implementation("androidx.core:core-ktx:1.17.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 testImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
 testImplementation("androidx.compose.ui:ui-test-junit4")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.16.1")
 testImplementation("androidx.test:core:1.7.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.room:room-testing:2.8.5")
}
