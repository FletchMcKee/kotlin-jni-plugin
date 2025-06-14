// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("Unused")

package io.github.fletchmckee.ktjni.samples.simple

import android.graphics.Bitmap

external fun Bitmap.gaussianBlur(sigma: Float): Bitmap
