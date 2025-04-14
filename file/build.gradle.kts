plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.javaext)
    api(Gson.gson)
    testImplementation(kotlinTestApi())
}