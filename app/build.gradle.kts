import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val supabaseUrl = providers.gradleProperty("SUPABASE_URL").orElse("").get()
val supabasePublishableKey = providers.gradleProperty("SUPABASE_PUBLISHABLE_KEY").orElse("").get()
val firebaseConfigExists = file("google-services.json").exists()
val appVersionName = providers.gradleProperty("APP_VERSION_NAME").orElse("0.9.17").get()
val appVersionCode = providers.gradleProperty("APP_VERSION_CODE").orElse("9017").get().toInt()

fun String.asBuildConfigString(): String =
    "\""+ replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.facevideo.app"
    compileSdk = 36

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ARTURCHAT_KEYSTORE_PATH")
            storeType = "JKS"
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
            }
            System.getenv("ARTURCHAT_KEYSTORE_PASSWORD")?.let { storePassword = it }
            System.getenv("ARTURCHAT_KEY_ALIAS")?.let { keyAlias = it }
            System.getenv("ARTURCHAT_KEY_PASSWORD")?.let { keyPassword = it }
        }
    }

    defaultConfig {
        applicationId = "com.facevideo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", supabasePublishableKey.asBuildConfigString())
        buildConfigField("Boolean", "FIREBASE_CONFIGURED", firebaseConfigExists.toString())
        buildConfigField(
            "Boolean",
            "SUPABASE_CONFIGURED",
            (supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()).toString()
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


 // The artwork is stored as UTF-8 base64 source and decoded before Android resource merging.
 // This keeps the exact same checked WebP pixels for the adaptive and standard launcher icons.
val launcherIconParts = fileTree("src/main/launcher-icon") {
    include("*.b64")
}
val generatedLauncherIconResources = layout.buildDirectory.dir("generated/launcherIconResources")
val generateLauncherIconResources = tasks.register("generateLauncherIconResources") {
    inputs.files(launcherIconParts)
    outputs.dir(generatedLauncherIconResources)
    doLast {
        val encoded = launcherIconParts.files
            .sortedBy { it.name }
            .joinToString("") { part -> part.readText().filterNot { it.isWhitespace() } }
        val imageBytes = Base64.getDecoder().decode(encoded)
        check(
            imageBytes.size > 1024 &&
                String(imageBytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(imageBytes, 8, 4, Charsets.US_ASCII) == "WEBP" &&
                String(imageBytes, 12, 4, Charsets.US_ASCII) in setOf("VP8 ", "VP8L", "VP8X") &&
                imageBytes.size == (ByteBuffer.wrap(imageBytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int + 8)
        ) { "A-Chat launcher artwork is not a complete WebP image." }

        val mipmapDirectory = generatedLauncherIconResources.get().dir("mipmap-nodpi").asFile
        mipmapDirectory.mkdirs()
        listOf("ic_launcher.webp", "ic_launcher_artwork.webp").forEach { fileName ->
            mipmapDirectory.resolve(fileName).writeBytes(imageBytes)
        }
    }
}
android.sourceSets.getByName("main").res.srcDir(generatedLauncherIconResources)
tasks.named("preBuild").configure {
    dependsOn(generateLauncherIconResources)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.11.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")

    // In-chat circular video notes: CameraX capture and Media3 playback.
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-video:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-okhttp:3.4.3")
    implementation("io.github.webrtc-sdk:android:150.7871.04")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

// Google Services is applied only when the Firebase config file is present.
if (firebaseConfigExists) {
    apply(plugin = "com.google.gms.google-services")
}
