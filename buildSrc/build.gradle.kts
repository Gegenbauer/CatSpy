plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(nokeeApi())
    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.10.1")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    nokee()
}

gradlePlugin {
    plugins {
        create("module-info-compile") {
            id = "module-info-compile"
            implementationClass = "ModuleInfoCompilePlugin"
        }
    }
}
