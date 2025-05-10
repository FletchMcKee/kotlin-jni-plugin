plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.0.21"
}

group = "io.github.fletchmckee"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-util:9.7")
}

gradlePlugin {
    plugins {
        create("generateKotlinJni") {
            id = "io.github.fletchmckee.kotlinjni"
            implementationClass = "io.github.fletchmckee.kotlinjni.KotlinJniPlugin"
        }
    }
}
