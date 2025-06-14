// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.samples.demo.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

suspend fun <T> suspendRunCatching(
  context: CoroutineContext,
  block: suspend () -> T,
): Result<T> = withContext(context) {
  try {
    Result.success(block())
  } catch (cancellationException: CancellationException) {
    throw cancellationException
  } catch (exception: Exception) {
    Result.failure(exception)
  }
}
