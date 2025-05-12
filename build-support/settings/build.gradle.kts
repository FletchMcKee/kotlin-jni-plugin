plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

gradlePlugin {
  plugins {
    register("ktjniSettings") {
      id = "io.github.fletchmckee.ktjni.settings"
      implementationClass = "io.github.fletchmckee.ktjni.buildsettings.KtjniSettingsPlugin"
    }
  }
}

