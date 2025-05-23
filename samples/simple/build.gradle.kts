// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  id("io.github.fletchmckee.ktjni")
}

android {
  namespace = "io.github.fletchmckee.ktjni.samples.simple"
  compileSdk =
    libs.versions.compileSdk
      .get()
      .toInt()

  defaultConfig {
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  externalNativeBuild {
    cmake {
      path("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }
}

dependencies {
  testImplementation(libs.junit)
}
