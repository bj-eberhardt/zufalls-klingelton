import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.emailspeicher.zufallsklingelton"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.emailspeicher.zufallsklingelton"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2Api35") {
                    device = "Pixel 2"
                    apiLevel = 35
                    systemImageSource = "aosp"
                    testedAbi = "x86_64"
                }
                create("pixel2Api36") {
                    device = "Pixel 2"
                    apiLevel = 36
                    systemImageSource = "aosp"
                    testedAbi = "x86_64"
                }
            }

        }
    }
}

tasks.register<Exec>("localPhoneMatrixTest") {
    group = "verification"
    description = "Runs Android instrumentation tests on all local managed phone devices."
    commandLine(
        "cmd.exe",
        "/c",
        rootProject.file("gradlew.bat").absolutePath,
        ":app:pixel2Api35DebugAndroidTest",
        ":app:pixel2Api36DebugAndroidTest",
        "-Pandroid.testInstrumentationRunnerArguments.notClass=de.emailspeicher.zufallsklingelton.PermissionAndPickerE2eTest,de.emailspeicher.zufallsklingelton.PlatformAcceptanceTest",
    )
}

tasks.register("localPermissionE2eTest") {
    group = "verification"
    description = "Runs real permission, Settings, and DocumentsUI E2E tests on local managed devices."
    dependsOn("pixel2Api35DebugAndroidTest", "pixel2Api36DebugAndroidTest")
}

tasks.register<Exec>("localPlatformAcceptanceTest") {
    group = "verification"
    description = "Runs real emulator phone-call and reboot acceptance tests on one connected emulator."
    dependsOn("installDebug", "installDebugAndroidTest")
    commandLine(
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        rootProject.file("tools/run-platform-acceptance.ps1").absolutePath,
    )
}

// Starting several emulators concurrently is unreliable on local Windows hosts.
// mustRunAfter only applies when the complete matrix task requests all devices;
// an individual device task remains independent.
tasks.configureEach {
    when (name) {
        "pixel2Api36DebugAndroidTest" -> mustRunAfter("pixel2Api35DebugAndroidTest")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
