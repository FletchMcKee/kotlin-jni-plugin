package io.github.fletchmckee.ktjni

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

internal val DirectoryProperty.inBuildDirectory: (Project) -> Boolean
  get() = { project ->
    val outputPath = this.get().asFile.toPath().normalize()
    val buildPath = project.layout.buildDirectory.get().asFile.toPath().normalize()
    outputPath.startsWith(buildPath)
  }
