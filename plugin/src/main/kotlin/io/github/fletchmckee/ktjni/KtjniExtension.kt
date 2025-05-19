// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty

@DslMarker
public annotation class KtjniDsl

/**
 * Configuration extension for the ktjni plugin.
 *
 * This extension allows customizing how JNI headers are generated from JVM language compilation outputs.
 */
@KtjniDsl
public interface KtjniExtension {
  /**
   * Specifies a custom output directory where JNI header files will be generated.
   *
   * Default: `{projectDir}/build/generated/sources/headers`.
   */
  public val outputDir: DirectoryProperty

  /**
   * Specifies build types to ignore during header generation.
   *
   * This is useful in Android projects or multi-variant builds where you want to skip header generation for certain build types
   * (e.g., "debug" or "androidTest").
   *
   * The matching is case-insensitive and works if the build type appears anywhere in the task name.
   *
   * Default: empty list (generate headers for all build types)
   */
  public val ignoreBuildTypes: ListProperty<String>
}
