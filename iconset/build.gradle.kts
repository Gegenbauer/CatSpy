plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.glog)
    compileOnly(projects.concurrency)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    compileOnly(JavaXAnno.groupName, JavaXAnno.annotationApi.artifact, JavaXAnno.annotationApi.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
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

tasks.compileJava.configure {
    dependsOn(generateIconAccessor)
}

tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
