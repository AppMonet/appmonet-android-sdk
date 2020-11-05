import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
  kotlin("multiplatform")
  kotlin("native.cocoapods")
  id("com.android.library")
  id("kotlin-android-extensions")
}

version = "1.0"

repositories {
  gradlePluginPortal()
  google()
  jcenter()
  mavenCentral()
  maven {
    url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
  }
}
kotlin {
  android()
  iosArm64("ios")
  cocoapods {
    summary = "AppMonet KMM DFP Library"
    homepage = "https://github.com/JetBrains/kotlin"
    frameworkName = "AppMonet_KMMM"
    ios.deploymentTarget = "10.0"
    pod("Google-Mobile-Ads-SDK", moduleName="GoogleMobileAds")
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":appmonet-sdk-core-kmm"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val androidMain by getting {
      dependencies {
        api("com.google.android.gms:play-services-ads:19.3.0")
        implementation("androidx.core:core-ktx:1.3.2")
      }
    }
    val androidTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation("junit:junit:4.12")
      }
    }
    val iosMain by getting
    val iosTest by getting
  }
}
android {
  compileSdkVersion(29)
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  defaultConfig {
    minSdkVersion(19)
    targetSdkVersion(29)
    versionCode = 1
    versionName = "1.0"
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}

//val packForXcode by tasks.creating(Sync::class) {
//  group = "build"
//  val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
//  val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
//  val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
//  val framework =
//    kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
//  inputs.property("mode", mode)
//  dependsOn(framework.linkTask)
//  val targetDir = File(buildDir, "xcode-frameworks")
//  from({ framework.outputDirectory })
//  into(targetDir)
//}
//tasks.getByName("build").dependsOn(packForXcode)
//
//val packForXcode by tasks.creating(Sync::class) {
//  group = "build"
//  val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
//
//  val framework = kotlin.targets.getByName<KotlinNativeTarget>("ios").binaries.getFramework(mode)
//
//  inputs.property("mode", mode)
//  dependsOn(framework.linkTask)
//  val targetDir = File(buildDir, "xcode-frameworks")
//  from({ framework.outputDirectory })
//  into(targetDir)
//}

//tasks.register("fatFramework", org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask::class) {
//  // The fat framework must have the same base name as the initial frameworks.
////  baseName = "${project.extra["appmonet-android-sdk"]}"
//  baseName = "AppMonet_KMM"
//  val frameworks = mutableListOf<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>()
//
//  kotlin.targets.withType<KotlinNativeTarget> {
//    print("JOSE HERE :: ${binaries.getFramework("RELEASE").baseName}")
//
//    frameworks.add(binaries.getFramework("RELEASE"))
//  }
//
//  // Specify the frameworks to be merged.
//  from(frameworks)
//}
//tasks.getByName("build").dependsOn(packForXcode)