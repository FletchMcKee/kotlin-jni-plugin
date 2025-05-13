// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  id("io.github.fletchmckee.ktjni")
}

android {
  namespace = "io.github.fletchmckee.ktjni.samples.simple"
  compileSdk = 35

  defaultConfig {
    minSdk = 26

    @Suppress("UnstableApiUsage")
    externalNativeBuild {
      cmake {
        cppFlags("")
      }
    }
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

  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {
  testImplementation(libs.junit)
}
