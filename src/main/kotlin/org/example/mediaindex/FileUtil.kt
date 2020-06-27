package org.example.mediaindex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.slf4j.LoggerFactory
import org.xml.sax.helpers.DefaultHandler
import java.awt.Desktop
import java.net.URI
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

data class TypedPath(val path: Path, val type: MediaType)

suspend fun readMusicMetaData(base: Path, channel: Channel<MusicMetaData>) {
    val files = flowFiles(base, FileVisitOption.FOLLOW_LINKS).filter { Files.isRegularFile(it) }.toChannel()
    coroutineScope {
        val readers = (0..Runtime.getRuntime().availableProcessors()).map {
            launch(Dispatchers.IO) {

                val logger = LoggerFactory.getLogger("MusicMetaDataReader-$it")

                val detector = DefaultDetector()
                val parser = AutoDetectParser(detector)
                val content = DefaultHandler()
                val ctx = ParseContext()

                for (path in files) {
                    Files.newInputStream(path, StandardOpenOption.READ).buffered().use {
                        val meta = Metadata().apply { add(Metadata.RESOURCE_NAME_KEY, path.toUri().toString()) }
                        val type = detector.detect(it, meta)
                        if (type.type == "audio") {
                            meta.apply { add(Metadata.CONTENT_TYPE, type.toString()) }
                            parser.parse(it, content, meta, ctx)
                            logger.debug { "Parsed MetaData for ${path.toAbsolutePath()}" }
                            channel.offer(MusicMetaData.extractFrom(path.toUri(), meta))
                        }
                    }
                }
            }
        }
        readers.onAllCompleted { channel.close(); LoggerFactory.getLogger("MusicMetaDataReader").info { "Close MusicMetaData Channel" } }

    }
}

fun flowFiles(base: Path, vararg options: FileVisitOption): Flow<Path> {
    val files = Files.walk(base, *options)
    return flow {
        for (path in files) {
            emit(path)
        }
    }.onCompletion { files.close(); println("Closing file walker") }
}
