plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Squareup.okhttp)
    implementation(Squareup.okhttpLoggingInterceptor)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(projects.file)
}