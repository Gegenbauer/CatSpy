plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(JavaXAnno.annotationApi)
    implementation(projects.resources)
    implementation(projects.file)
}

val generateGlobalProperties by tasks.registering {
    val generatedDir = project.buildDir.resolve("generated/globalProperties")
    sourceSets.main.configure {
        java.srcDir(generatedDir)
    }
    doFirst {
        sourceSets.main.configure {

            val filename = "GlobalProperties"
            generatedDir.mkdirs()
            val packageName = "me.gegenbauer.catspy.platform"
            generatedDir.resolve("${packageName.replace('.', '/')}/$filename.java").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(generateGlobalPropertiesAccessor(project))
            }
        }
    }
}

tasks.compileKotlin.configure {
    dependsOn(generateGlobalProperties)
}

tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}