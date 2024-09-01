plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(kotlinReflectApi())
    testImplementation(kotlinTestApi())
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}