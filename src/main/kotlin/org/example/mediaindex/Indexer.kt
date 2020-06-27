package org.example.mediaindex

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.apache.lucene.index.*
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.xml.sax.helpers.DefaultHandler
import java.io.Closeable
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger


class Indexer(location: Path) : Closeable {

    private val idxDir: Directory = FSDirectory.open(location)
    private val idxWriterConfig = IndexWriterConfig()
    private val idxWriter = IndexWriter(idxDir, idxWriterConfig)
    private val idxReader = DirectoryReader.open(idxWriter)
    private val idxSearcher = IndexSearcher(idxReader)

    private val metaDataParser = AutoDetectParser()

    suspend fun index(path: Path) = coroutineScope {
        val count = AtomicInteger(0)
        val channel = Channel<MusicMetaData>(capacity = Channel.UNLIMITED)

        val reader = launch { readMusicMetaData(path, channel) }

        (0..Runtime.getRuntime().availableProcessors()).map {
            launch { indexMusicMeta(channel, count) }
        }.joinAll()

        reader.join()

        count.get()
    }

    suspend fun indexMusicMeta(channel: Channel<MusicMetaData>, count: AtomicInteger) {
        val doc = MusicMetaDataDoc()
        for (meta in channel) {
            doc.update(meta)

            val top = idxSearcher.search(TermQuery(Term("uri", meta.uri.toString())), 1)

            if (top.totalHits.value > 0) {
                idxWriter.tryDeleteDocument(idxReader, top.scoreDocs.first().doc)
            }
            idxWriter.addDocument(doc.doc)

            count.incrementAndGet()
        }
    }

    override fun close() {
        idxWriter.close()
        idxDir.close()
    }
}

