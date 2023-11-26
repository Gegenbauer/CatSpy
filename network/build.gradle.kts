plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Squareup.okhttp)
    implementation(Squareup.okhttpLoggingInterceptor)
    implementation(Gson.gson)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.javaext)
}