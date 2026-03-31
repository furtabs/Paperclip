plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.furtabs.paperclip"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.furtabs.paperclip"
        minSdk = 29
        targetSdk = 36
        versionCode = 50500
        versionName = "5.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    fun loadDotEnv(): Map<String, String> {
        val envFile = rootProject.file(".env")
        if (!envFile.exists()) return emptyMap()
        return envFile.readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
                val delimiterIndex = trimmed.indexOf('=')
                if (delimiterIndex < 0) return@mapNotNull null
                val key = trimmed.substring(0, delimiterIndex).trim()
                var value = trimmed.substring(delimiterIndex + 1).trim()
                value = value.trim('"', '\'')
                key to value
            }
            .toMap()
    }

    val dotEnv = loadDotEnv()
    val uspsApiKeyValue = dotEnv["USPS_API_KEY"] ?: System.getenv("USPS_API_KEY") ?: project.findProperty("USPS_API_KEY")?.toString() ?: ""
    val uspsClientKeyValue = dotEnv["USPS_CLIENT_KEY"] ?: System.getenv("USPS_CLIENT_KEY") ?: project.findProperty("USPS_CLIENT_KEY")?.toString() ?: ""
    val uspsClientSecretValue = dotEnv["USPS_CLIENT_SECRET"] ?: System.getenv("USPS_CLIENT_SECRET") ?: project.findProperty("USPS_CLIENT_SECRET")?.toString() ?: ""
    val uspsTokenUrlValue = dotEnv["USPS_OAUTH_TOKEN_URL"] ?: System.getenv("USPS_OAUTH_TOKEN_URL") ?: project.findProperty("USPS_OAUTH_TOKEN_URL")?.toString() ?: "https://apis.usps.com/oauth2/v3/token"

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            buildConfigField("String", "USPS_API_KEY", "\"$uspsApiKeyValue\"")
            buildConfigField("String", "USPS_CLIENT_KEY", "\"$uspsClientKeyValue\"")
            buildConfigField("String", "USPS_CLIENT_SECRET", "\"$uspsClientSecretValue\"")
            buildConfigField("String", "USPS_OAUTH_TOKEN_URL", "\"$uspsTokenUrlValue\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "USPS_API_KEY", "\"$uspsApiKeyValue\"")
            buildConfigField("String", "USPS_CLIENT_KEY", "\"$uspsClientKeyValue\"")
            buildConfigField("String", "USPS_CLIENT_SECRET", "\"$uspsClientSecretValue\"")
            buildConfigField("String", "USPS_OAUTH_TOKEN_URL", "\"$uspsTokenUrlValue\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.3")
    implementation("com.kizitonwose.calendar:compose:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha03")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(libs.androidx.ui.graphics)
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.materialkolor:material-kolor:4.0.5")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("com.google.re2j:re2j:1.8")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material3:material3:1.5.0-alpha15")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
