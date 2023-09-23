plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Weisj.darklafCore)
    implementation(projects.glog)
    implementation(projects.ui.utils)
    implementation(projects.javaext)
    api(Kotlin.coroutineCore)
    api(Kotlin.coroutineSwing)
    api(Fifesoft.autocomplete)

    testImplementation(kotlinTestApi())
    testImplementation(projects.ui.utils)
    testImplementation(kotlinReflectApi())
    testImplementation(Mockk.mockk)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}