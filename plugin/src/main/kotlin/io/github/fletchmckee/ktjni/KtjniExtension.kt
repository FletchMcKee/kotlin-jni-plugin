// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.file.DirectoryProperty

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
   * Base output directory for generated JNI headers.
   *
   * Files are placed under `{outputDir}/{sourceType}/{sourceName}` to guarantee cache correctness.
   *
   * Default: `{project.projectDir}/build/generated/sources/headers`
   */
  public val outputDir: DirectoryProperty
}
