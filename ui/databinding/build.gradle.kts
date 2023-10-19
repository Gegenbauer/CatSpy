import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.ui.utils)
    implementation(projects.javaext)
    implementation(Weisj.darklafCore)
    testImplementation(projects.concurrency)
    testImplementation(projects.glog)
    testImplementation(Weisj.darklafCore)
    testImplementation(JGoodies.binding)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}