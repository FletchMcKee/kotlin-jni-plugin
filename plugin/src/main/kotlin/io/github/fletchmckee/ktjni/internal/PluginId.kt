// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.internal

internal enum class PluginId(internal val id: String) {
  KotlinAndroid("org.jetbrains.kotlin.android"),
  KotlinJvm("org.jetbrains.kotlin.jvm"),
  KotlinMultiplatform("org.jetbrains.kotlin.multiplatform"),

  AndroidApplication("com.android.application"),
  AndroidLibrary("com.android.library"),

  Scala("scala"),
}
