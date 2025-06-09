// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.util

internal const val GROUP = "ktjni"

internal fun String.taskSuffix(target: String): String = if (target.isEmpty()) this else target + titleCase

internal val String.titleCase: String get() = replaceFirstChar(Char::uppercase)
