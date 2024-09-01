plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.javaext)
    api(Kotlin.coroutineCore)
    api(Kotlin.coroutineSwing)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}