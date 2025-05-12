// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "build-support"
include(":plugin")
project(":plugin").projectDir = File("../plugin")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
