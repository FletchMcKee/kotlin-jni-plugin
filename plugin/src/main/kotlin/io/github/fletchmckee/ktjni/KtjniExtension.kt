// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import org.gradle.api.file.DirectoryProperty

@DslMarker
public annotation class KtjniDsl

@KtjniDsl
public interface KtjniExtension {
  public val outputDir: DirectoryProperty
}
