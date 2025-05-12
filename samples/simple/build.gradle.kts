// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
buildscript {
  dependencies {
    classpath(libs.ktjni.gradle.plugin)
  }
  repositories {
    mavenLocal()
    mavenCentral()
    google()
  }
}

apply(plugin = "io.github.fletchmckee.ktjni")

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ktjni.root)
}

android {
  namespace = "io.github.fletchmckee.ktjni.samples.simple"
  compileSdk = 35

  defaultConfig {
    minSdk = 26

    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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
  androidTestImplementation(libs.runner)
  androidTestImplementation(libs.espresso.core)
}
