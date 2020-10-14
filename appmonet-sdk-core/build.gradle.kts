import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization") version "1.4.10"
  id("com.codingfeline.buildkonfig")
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
  }
}
buildkonfig {
  packageName = "com.monet"
  // exposeObjectWithName = 'YourAwesomePublicConfig'

  defaultConfigs {
    buildConfigField(STRING, "STAGING_SERVER_HOST", "url")
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