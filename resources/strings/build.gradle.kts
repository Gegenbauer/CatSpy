plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(JavaXAnno.annotationApi)
    implementation(Gson.gson)
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

            fun getStringResourceFiles(): List<File> {
                val result = arrayListOf<File>()
                val stringResourceDir = project.file("src/main/resources/strings/")
                stringResourceDir.takeIf { it.isDirectory }?.run {
                    listFiles()?.filter { it.isFile && it.extension == "json" }?.let { files ->
                        result.addAll(files)
                    }
                }
                return result
            }

            val strJson = project.file("default.json")
            val strJsonFilesToSort = mutableListOf(strJson)
            strJsonFilesToSort.addAll(getStringResourceFiles())
            println("sort json properties: ${strJsonFilesToSort.map { it.name }}")
            strJsonFilesToSort.forEach { it.writeText(sortProperties(it).toPrettyString()) }
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

tasks.compileKotlin.configure {
    dependsOn(generateStringAccessor)
}

tasks.getByName<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}