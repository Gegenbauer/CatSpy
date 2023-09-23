plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
    id(SqlDelight.groupName)
}

dependencies {
    implementation(SqlDelight.sqliteDriver)
    implementation(SqlDelight.coroutines)
    implementation(projects.ui.utils)

    testImplementation(kotlinTestApi())
    testImplementation(SqlDelight.sqliteDriver)
    testImplementation(SqlDelight.coroutines)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

sqldelight {
    database("Database") {
        packageName = "me.gegenbauer.catspy.database"
    }
}