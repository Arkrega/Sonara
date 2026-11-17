import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val regaProperties = Properties()
val regaPropertiesFile = rootProject.file("rega.properties")
if (regaPropertiesFile.exists()) {
    regaProperties.load(regaPropertiesFile.inputStream())
}

fun resolveOriginRepo(): Pair<String, String>? {
    return runCatching {
        val gitConfigFile = rootProject.file(".git/config")
        if (!gitConfigFile.exists()) return@runCatching null
        val content = gitConfigFile.readText()
        val url = Regex("""\[remote\s+"origin"\][^\[]*?url\s*=\s*(\S+)""", RegexOption.MULTILINE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.removeSuffix(".git")
            ?.trimEnd('/')
            ?: return@runCatching null
        val path = url.substringAfter("github.com", missingDelimiterValue = "")
            .trim(':', '/')
        val parts = path.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return@runCatching null
        val owner = parts[0]
        val repo = parts[1]
        if (owner.equals("MetrolistGroup", ignoreCase = true) && repo.equals("Metrolist", ignoreCase = true)) {
            null
        } else {
            owner to repo
        }
    }.getOrNull()
}

val detectedRepo = resolveOriginRepo()
val regaAppId = regaProperties.getProperty("rega.applicationId") ?: "com.arkrega.sonara"
val regaAppName = regaProperties.getProperty("rega.applicationName") ?: "SONARA"
val regaVersionName = regaProperties.getProperty("rega.versionName") ?: "1.0.0"
val regaVersionCode = regaProperties.getProperty("rega.versionCode")?.toIntOrNull() ?: 1
val githubOwner = System.getenv("REGA_GITHUB_OWNER")
    ?: detectedRepo?.first
    ?: regaProperties.getProperty("rega.github.owner")
    ?: "arkrega"
val githubRepo = System.getenv("REGA_GITHUB_REPO")
    ?: detectedRepo?.second
    ?: regaProperties.getProperty("rega.github.repo")
    ?: "sonara"
val websiteUrl = System.getenv("REGA_WEBSITE_URL")
    ?: regaProperties.getProperty("rega.website.url")
    ?: "https://$githubOwner.github.io/$githubRepo/"

val baseApplicationId = regaAppId
val baseAppName = regaAppName
val applicationIdOverride = System.getenv("SONARA_APPLICATION_ID")
    ?: System.getenv("REGA_APPLICATION_ID")
val appNameOverride = System.getenv("SONARA_APP_NAME")
    ?: System.getenv("REGA_APP_NAME")
val buildCommit =
    (System.getenv("SONARA_BUILD_COMMIT") ?: System.getenv("REGA_BUILD_COMMIT") ?: System.getenv("METROLIST_BUILD_COMMIT"))
        ?.trim()
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
        ?.take(7)
        ?.lowercase()
val debugKeystorePathOverride =
    System.getenv("SONARA_DEBUG_KEYSTORE_PATH")
        ?: System.getenv("REGA_DEBUG_KEYSTORE_PATH")
        ?: System.getenv("METROLIST_DEBUG_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val debugKeystorePassword =
    System.getenv("SONARA_DEBUG_KEYSTORE_PASSWORD")
        ?: System.getenv("REGA_DEBUG_KEYSTORE_PASSWORD")
        ?: System.getenv("METROLIST_DEBUG_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: "android"
val debugKeyAlias =
    System.getenv("SONARA_DEBUG_KEY_ALIAS")
        ?: System.getenv("REGA_DEBUG_KEY_ALIAS")
        ?: System.getenv("METROLIST_DEBUG_KEY_ALIAS")?.takeIf { it.isNotBlank() }
        ?: "androiddebugkey"
val debugKeyPassword =
    System.getenv("SONARA_DEBUG_KEY_PASSWORD")
        ?: System.getenv("REGA_DEBUG_KEY_PASSWORD")
        ?: System.getenv("METROLIST_DEBUG_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: "android"
// Debug installs are shipped through CI and installed on phones as the regular
// app, so by default they are shrunk to the same size class as release. Pass
// -Psonara.debuggable to get a fully debuggable, unshrunk build for development.
val debuggableDebugBuilds = providers.gradleProperty("sonara.debuggable").isPresent

val persistentDebugKeystoreFile = file("persistent-debug.keystore")
val workflowDebugKeystoreFile = debugKeystorePathOverride?.let(::file)

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.metrolist.music"
    compileSdk = 37

    defaultConfig {
        applicationId = applicationIdOverride ?: baseApplicationId
        minSdk = 26
        targetSdk = 36
        versionCode = regaVersionCode
        versionName = regaVersionName
        val baseVersionName = requireNotNull(versionName)
        buildConfigField("String", "BASE_VERSION_NAME", "\"$baseVersionName\"")
        buildCommit?.let { versionName = "$baseVersionName+$it" }
        resValue("string", "app_name", appNameOverride ?: baseAppName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")
        buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        buildConfigField("Long", "DISCORD_APP_ID", "1447278780795064401L")
        buildConfigField("String", "APP_NAME", "\"${appNameOverride ?: baseAppName}\"")
        buildConfigField("String", "GITHUB_OWNER", "\"$githubOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
        buildConfigField("String", "WEBSITE_URL", "\"$websiteUrl\"")
    }

    flavorDimensions += listOf("variant")
    productFlavors {
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        create("izzy") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "false")
        }
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = persistentDebugKeystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("workflowDebug") {
            storeFile = workflowDebugKeystoreFile ?: persistentDebugKeystoreFile
            storePassword = debugKeystorePassword
            keyAlias = debugKeyAlias
            keyPassword = debugKeyPassword
        }
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseKeystore = file("keystore/release.keystore")
            if (releaseKeystore.exists() && !System.getenv("STORE_PASSWORD").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            if (applicationIdOverride == null) {
                applicationIdSuffix = ".debug"
            }
            isDebuggable = debuggableDebugBuilds
            // Obfuscation stays off (`-dontobfuscate`), so stack traces and R8
            // rules behave normally even in the shrunk variant.
            isMinifyEnabled = !debuggableDebugBuilds
            isShrinkResources = !debuggableDebugBuilds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (appNameOverride == null) {
                resValue("string", "app_name", baseAppName)
            }
            signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else if (persistentDebugKeystoreFile.exists()) {
                    signingConfigs.getByName("persistentDebug")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
        checkReleaseBuilds = false
    }

    androidResources {
        generateLocaleConfig = true
        // Ship a curated set of locales instead of every community translation.
        // Every dropped locale removes a full string table from resources.arsc;
        // this trims the APK by roughly 2 MB while keeping the major languages.
        localeFilters += listOf(
            "en", "in", "es", "pt-rBR", "fr", "de", "it", "ru",
            "tr", "ar", "hi", "ja", "ko", "zh-rCN", "zh-rTW", "vi", "th",
            "pl", "nl",
        )
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols +=
                listOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}

val cleanLegacyProtoSources = tasks.register<Delete>("cleanLegacyProtoSources") {
    delete(layout.projectDirectory.dir("src/main/java/com/metrolist/music/listentogether/proto"))
}

tasks.named("preBuild") {
    dependsOn(cleanLegacyProtoSources)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
        suppressWarnings.set(false)
    }
}

configurations.configureEach {
    exclude(group = "org.json", module = "json")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.palette)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.browser)

    implementation(libs.ucrop)


    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    "gmsImplementation"(libs.media3.cast)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.ktor.client.mock)
}
