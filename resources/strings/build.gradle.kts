import com.google.gson.GsonBuilder

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(JavaXAnno.groupName, JavaXAnno.annotationApi.artifact, JavaXAnno.annotationApi.version)
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    implementation(projects.file)
    implementation(projects.resources.common)
    implementation(projects.ui.utils)
    implementation(projects.javaext)
}

val generateStringAccessor by tasks.registering {
    val generatedDir = project.buildDir.resolve("generated/stringAccessor")
    sourceSets.main.configure {
        java.srcDir(generatedDir)
    }
    doFirst {
        sourceSets.main.configure {
            val strJson = project.file("default.json")
            strJson.writeText(sortProperties(strJson).toPrettyString())
            val allStringsName = "Strings"
            generatedDir.mkdirs()
            val packageName = "me.gegenbauer.catspy.strings"
            generatedDir.resolve("${packageName.replace('.', '/')}/$allStringsName.java").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(createStringAccessor(strJson, packageName, allStringsName))
            }
        }
    }
}

tasks.compileJava.configure {
    dependsOn(generateStringAccessor)
}

tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}