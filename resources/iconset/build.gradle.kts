plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(JavaXAnno.annotationApi)
    implementation(Weisj.darklafCore)
    implementation(FormDev.extra)
}

val generateIconAccessor by tasks.registering {
    val generatedDir = project.buildDir.resolve("generated/iconAccessor")
    sourceSets.main.configure {
        java.srcDir(generatedDir)
    }
    doFirst {
        sourceSets.main.configure {
            val propertyFile = project.file("iconAccessorSpec.properties")
            val allIconsName = "GIcons"
            generatedDir.mkdirs()
            val packageName = "me.gegenbauer.catspy.iconset"
            generatedDir.resolve("${packageName.replace('.', '/')}/$allIconsName.java").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(createIconAccessor(propertyFile, packageName, allIconsName))
            }
        }
    }
}

tasks.compileKotlin.configure {
    dependsOn(generateIconAccessor)
}

tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
