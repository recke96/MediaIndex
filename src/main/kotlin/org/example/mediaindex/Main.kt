package org.example.mediaindex

import dev.dirs.ProjectDirectories
import dev.dirs.UserDirectories
import kotlinx.coroutines.*
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexNotFoundException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
fun main(args: Array<String>) {
    val project = ProjectDirectories.from("org", "example", "mediaindex")
    val musicDir = Path.of(UserDirectories.get().audioDir)
    val dataDir = Path.of(project.dataDir)

    when (args.firstOrNull()) {
        null -> usage()
        "index" -> index(musicDir, dataDir)
        "query" -> query(dataDir, args.drop(1).joinToString(separator = " "))
        "clear" -> clear(dataDir)
        else -> usage()
    }

}

fun usage(): Nothing {
    System.err.println(
        """
        Usage: java -jar {jarfile} {Command} [Query]
        Commands:
          index - Create a media index for the operating system music folder
          clear - Delete the current index (if it exists)
          query - Search for a song with the specified [Query].
    """.trimIndent()
    )
    exitProcess(-1)
}

@ExperimentalTime
fun index(targetDir: Path, indexDir: Path) {
    Indexer(indexDir).use {
        runBlocking {
            var count = -1
            val dur = measureTime {
                val indexing = async {
                    it.index(targetDir)
                }

                println("Indexing $targetDir, index is stored at $indexDir")
                while (!indexing.isCompleted) {
                    delay(250)
                    print(".")
                }
                println()

                count = indexing.await()
            }

            println("$count documents added to index in $dur")
        }
    }
}

@ExperimentalTime
fun query(indexDir: Path, query: String) {

    val result = mutableListOf<QueryResult>()
    val dur = measureTime {
        val parsedQuery = QueryParser("title", StandardAnalyzer()).parse(query)
        try {
            DirectoryReader.open(FSDirectory.open(indexDir)).use {
                val searcher = IndexSearcher(it)
                val top = searcher.search(parsedQuery, 10)
                for (score in top.scoreDocs) {
                    result.add(QueryResult(searcher.doc(score.doc), score.score))
                }
            }
        } catch (_: IndexNotFoundException) {
            System.err.println("No index found. Create an index using 'mediaindex index'.")
            exitProcess(-1)
        }
    }

    if (result.isEmpty()) {
        println("Could not find songs for query $query")
        exitProcess(0)
    } else {
        println("Found top ${result.size} matches in $dur")

        result.forEachIndexed { index, queryResult ->
            println("=== ${index + 1}: ${queryResult.score} ===")
            MusicMetaData.extractFrom(queryResult.doc).print()
            println()
        }
    }
}

data class QueryResult(val doc: Document, val score: Float)

@ExperimentalTime
fun clear(indexDir: Path) {
    if (Files.exists(indexDir)) {
        println("Deleting existing index at $indexDir")
        runBlocking {
            var count = 0;
            val dur = measureTime {
                val job = launch(Dispatchers.IO) {
                    Files.walk(indexDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.delete(it).also { count++ } }
                }
                while (!job.isCompleted) {
                    delay(250)
                    print(".")
                }
                println()
            }
            println("Deleted $count files and directories in $dur")
        }
    } else {
        println("No index found at $indexDir")
    }
}
