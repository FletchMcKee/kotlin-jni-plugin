// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

internal fun deleteBuild(parent: File) {
  File(parent, "build").deleteRecursively()
}

internal fun assertKotlinAndroidTestsNoSource(result: BuildResult) {
  // There is no ReleaseAndroidTest variant.
  assertThat(result.task(":generateKotlinDebugAndroidTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateKotlinDebugUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateKotlinReleaseUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
}

internal fun String.withInvariantPathSeparators() = replace("\\", "/")
