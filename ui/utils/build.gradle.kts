plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(Weisj.darklafCore)
    implementation(Zip4j.zip4j)
}