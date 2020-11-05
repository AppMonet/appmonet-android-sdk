import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization") version "1.4.10"
  id("com.android.library")
  id("kotlin-android-extensions")
}


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
//  iosArm64 { binaries.framework("AppMonet_Core") }
//  iosX64 {
//    binaries.framework("AppMonet_Core")
  ios {
    binaries {
      framework {
        baseName = "AppMonet_Core"
      }
    }
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
        implementation("co.touchlab:stately-common:1.1.0")
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
//    getByName("iosArm64Main") { dependsOn(iosMain) }
//    getByName("iosArm64Test") { dependsOn(iosTest) }
//    getByName("iosX64Main") { dependsOn(iosMain) }
//    getByName("iosX64Test") { dependsOn(iosTest) }
  }

  tasks {
//    register("universalFrameworkDebug", org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask::class) {
//      baseName = "AppMonet_Core"
//      from(
//          iosArm64().binaries.getFramework("AppMonet_Core", "Debug"),
//          iosX64().binaries.getFramework("AppMonet_Core", "Debug")
//      )
//      destinationDir = buildDir.resolve("bin/universal/debug")
//      group = "Universal framework"
//      description = "Builds a universal (fat) debug framework"
//      dependsOn("linkAppMonet_CoreDebugFrameworkIosArm64")
//      dependsOn("linkAppMonet_CoreDebugFrameworkIosX64")
//    }
//    register(
//        "universalFrameworkRelease", org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask::class
//    ) {
//      baseName = "AppMonet_Core"
//      from(
//          iosArm64().binaries.getFramework("AppMonet_Core", "Release"),
//          iosX64().binaries.getFramework("AppMonet_Core", "Release")
//      )
//      destinationDir = buildDir.resolve("bin/universal/release")
//      group = "Universal framework"
//      description = "Builds a universal (fat) release framework"
//      dependsOn("linkAppMonet_CoreReleaseFrameworkIosArm64")
//      dependsOn("linkAppMonet_CoreReleaseFrameworkIosX64")
//    }
//
//    register("universalFramework") {
//      dependsOn("universalFrameworkDebug")
//      dependsOn("universalFrameworkRelease")
//    }
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
val packForXcode by tasks.creating(Sync::class) {
  group = "build"
  val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
  val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
  val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
  val framework =
    kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
  inputs.property("mode", mode)
  dependsOn(framework.linkTask)
  val targetDir = File(buildDir, "xcode-frameworks")
  from({ framework.outputDirectory })
  into(targetDir)
}
tasks.getByName("build").dependsOn(packForXcode)
