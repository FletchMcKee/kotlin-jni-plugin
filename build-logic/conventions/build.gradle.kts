plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
}

group = "io.github.fletchmckee.build-logic"

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
    licenseHeaderFile(rootProject.file("../../spotless/copyright.txt"))
  }

  kotlinGradle {
    target("*.kts")
    ktlint()
    licenseHeaderFile(rootProject.file("../../spotless/copyright.txt"), "(^(?![\\/ ]\\**).*$)")
  }
}

dependencies {
  compileOnly(libs.spotless.gradlePlugin)
}

gradlePlugin {
  plugins {
    create("generateKtjni") {
      id = "io.github.fletchmckee.ktjni"
      implementationClass = "io.github.fletchmckee.ktjni.KotlinJniPlugin"
    }

    register("root") {
      id = libs.plugins.ktjni.root.get().pluginId
      implementationClass = "io.github.fletchmckee.ktjni.RootConventionPlugin"
    }
  }
}
