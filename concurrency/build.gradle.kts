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