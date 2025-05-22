// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

@CacheableTask
internal abstract class KtjniWorkerTask : DefaultTask() {
  @get:Inject
  internal abstract val workerExecutor: WorkerExecutor

  internal fun workQueue(): WorkQueue = workerExecutor.noIsolation()
}
