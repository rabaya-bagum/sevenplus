plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun prop(name: String): String = (findProperty("sevenplus.$name") as String?).orEmpty()

android {
    namespace = "com.sevenplus.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sevenplus.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"${prop("supabaseUrl")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${prop("supabaseAnonKey")}\"")
        buildConfigField("String", "SENTRY_DSN", "\"${prop("sentryDsn")}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${prop("privacyPolicyUrl")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The 3D exercise clips are shared with iOS: assets/ExerciseClips/<id>.mp4, <id>-dark.mp4,
    // <id>-thumb.jpg, <id>-dark-thumb.jpg (rendered by tools/animation).
    sourceSets.getByName("main").assets.srcDir("../../SevenPlus/Resources")

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.health.connect)
    implementation(libs.billing.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.sentry.android)
    implementation(libs.mlkit.pose)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
