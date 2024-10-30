plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.javaext)
    api(Gson.gson)
    api(Squareup.okio)
    api(ApacheCommons.compress)
    api(Junrar.junrar)
    api(Tukaani.xz)
}