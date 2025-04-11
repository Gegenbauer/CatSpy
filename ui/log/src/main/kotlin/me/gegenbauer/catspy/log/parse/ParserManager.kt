package me.gegenbauer.catspy.log.parse

import io.github.classgraph.ClassGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.platform.currentPlatform
import java.io.File
import java.util.concurrent.CountDownLatch

class ParserManager : ContextService {
    private val builtInParsers = mutableMapOf<String, ParserEntry>()
    private val externalParsers = mutableMapOf<String, ParserEntry>()
    private val parserDirPath by lazy {
        currentPlatform.getFilesDir().appendPath(PARSER_DIR_NAME)
    }
    private val parserLoadedCountDownLatch = CountDownLatch(1)

    suspend fun loadParsers() {
        loadBuiltInParsers()
        loadExternalParsers()
        parserLoadedCountDownLatch.countDown()
    }

    fun getParser(clazzName: String, jarPath: String, isBuiltIn: Boolean): SequenceLogParser? {
        parserLoadedCountDownLatch.await()
        return if (isBuiltIn) {
            builtInParsers[clazzName]?.parser
        } else {
            externalParsers[createExternalParserKey(clazzName, jarPath)]?.parser
        }
    }

    fun getAllParsers(): List<ParserEntry> {
        parserLoadedCountDownLatch.await()
        return builtInParsers.values.toList() + externalParsers.values.toList()
    }

    private suspend fun loadBuiltInParsers() {
        withContext(Dispatchers.GIO) {
            val targetPackage = this@ParserManager::class.java.`package`.name
            val parserClasses = loadImplementationsFromPackage(targetPackage, SequenceLogParser::class.java)
            parserClasses.forEach { parserClass ->
                val parser = parserClass.getDeclaredConstructor().newInstance() as SequenceLogParser
                addBuiltInParser(parser)
            }
        }
    }

    private suspend fun loadImplementationsFromPackage(packageName: String, interfaceClass: Class<*>): List<Class<*>> {
        return withContext(Dispatchers.GIO) {
            ClassGraph()
                .enableClassInfo()
                .acceptPackages(packageName)
                .scan()
                .use { scanResult ->
                    runCatching {
                        scanResult.getClassesImplementing(interfaceClass.name)
                            .loadClasses(interfaceClass)
                            .toList()
                    }.onFailure {
                        Log.e(TAG, "[loadImplementationsFromPackage] Error loading implementations from package.", it)
                    }.getOrDefault(emptyList())
                }
        }
    }

    private suspend fun loadExternalParsers() {
        withContext(Dispatchers.GIO) {
            val targetDir = File(parserDirPath)
            if (!targetDir.exists()) {
                Log.d(TAG, "[loadExternalParsers] Parser directory does not exist. Creating it.")
                targetDir.mkdirs()
            }
            val parserClasses = loadImplementationsFromJars(parserDirPath, SequenceLogParser::class.java)
            parserClasses.forEach { (jarPath, parserClass) ->
                val parser = parserClass.getDeclaredConstructor().newInstance() as SequenceLogParser
                addExternalParser(parser, jarPath)
            }
        }
    }

    suspend fun loadImplementationsFromJars(directory: String, interfaceClass: Class<*>): List<Pair<String, Class<*>>> {
        return withContext(Dispatchers.GIO) {
            runCatching {
                val jarFiles = File(directory).listFiles { file -> file.extension == PARSER_FILE_EXTENSION }
                    ?: return@withContext emptyList()
                ClassGraph()
                    .enableClassInfo()
                    .overrideClasspath(*jarFiles.map { it.absolutePath }.toTypedArray())
                    .scan()
                    .use { scanResult ->
                        runCatching {
                            scanResult.getClassesImplementing(interfaceClass.name)
                                .loadClasses(interfaceClass)
                                .toList().map { scanResult.classpath to it }
                        }.onFailure {
                            Log.e(TAG, "[loadImplementationsFromJars] Error loading implementations from jars.", it)
                        }.getOrDefault(emptyList())
                    }
            }.onFailure {
                Log.e(TAG, "[loadImplementationsFromJars] Error loading implementations from jars.", it)
            }.getOrDefault(emptyList())
        }
    }

    @Synchronized
    private fun addBuiltInParser(parser: SequenceLogParser) {
        Log.d(TAG, "[addBuiltInParser] Adding built-in parser: ${parser.javaClass.canonicalName}")
        builtInParsers[parser.javaClass.canonicalName] =
            ParserEntry(parser, parser.javaClass.canonicalName, isBuiltIn = true)
    }

    @Synchronized
    private fun addExternalParser(parser: SequenceLogParser, jarPath: String) {
        Log.d(TAG, "[addExternalParser] Adding external parser: ${parser.javaClass.canonicalName}, jarPath: $jarPath")
        externalParsers[createExternalParserKey(parser.javaClass.canonicalName, jarPath)] =
            ParserEntry(parser, parser.javaClass.canonicalName, jarPath)
    }

    private fun createExternalParserKey(clazzName: String, jarPath: String): String {
        return "$clazzName-$jarPath"
    }

    companion object {
        private const val TAG = "ParserManager"
        private const val PARSER_DIR_NAME = "parsers"
        private const val PARSER_FILE_EXTENSION = "jar"
    }
}

data class ParserEntry(
    val parser: SequenceLogParser,
    val clazzName: String,
    val jarPath: String = "",
    val isBuiltIn: Boolean = false,
)