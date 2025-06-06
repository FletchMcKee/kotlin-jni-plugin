// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.util

internal fun String.taskSuffix(target: String): String = if (target.isEmpty()) this else target + this.titleCase

internal val String.titleCase: String get() = this.replaceFirstChar(Char::uppercase)
