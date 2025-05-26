// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.tasks

import io.github.fletchmckee.ktjni.internal.GenerateJniHeaders
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class KtjniTask
@Inject
constructor() : KtjniWorkerTask() {
  @get:InputDirectory
  @get:SkipWhenEmpty
  @get:IgnoreEmptyDirectories
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generateJniHeaders() {
    workQueue().submit(GenerateJniHeaders::class.java) {
      sourceDir.set(this@KtjniTask.sourceDir)
      outputDir.set(this@KtjniTask.outputDir)
    }
  }
}
