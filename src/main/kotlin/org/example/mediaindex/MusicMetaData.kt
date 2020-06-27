package org.example.mediaindex

import org.apache.lucene.document.*
import org.apache.tika.metadata.Metadata
import java.io.PrintStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

data class MusicMetaData(
    val uri: URI,
    val artist: String?,
    val albumArtist: String?,
    val title: String?,
    val album: String?,
    val genre: String?,
    val duration: Double
) {

    companion object {


        fun extractFrom(uri: URI, meta: Metadata): MusicMetaData {
            val artist = meta.getValues("xmpDM:artist").joinToString(separator = "; ")
            val albumArtist = meta.getValues("albumartist").joinToString(separator = "; ")
            val title = meta.get("title") ?: ""
            val album = meta.get("xmpDM:album") ?: ""
            val genre = meta.getValues("xmpDM:genre").joinToString(separator = "; ")
            val duration = meta.get("xmpDM:duration")?.toDouble() ?: -1.0

            return MusicMetaData(uri, artist, albumArtist, title, album, genre, duration)
        }

        fun extractFrom(doc: Document): MusicMetaData {
            val uri = URI(doc["uri"])
            val artist = doc["artist"]
            val albumArtist = doc["albumartist"]
            val album = doc["album"]
            val title = doc["title"]
            val genre = doc["genre"]
            val duration = doc["duration_s"]?.toDouble() ?: -1.0

            return MusicMetaData(uri, artist, albumArtist, title, album, genre, duration)
        }
    }

    @ExperimentalTime
    fun print(out: PrintStream = System.out) {
        val d = duration.seconds.toComponents { min, sec, ns ->
            val totalSec = sec + ns * 10.0.pow(-9.0)
            "$min:${"%02.0f".format(totalSec)}"
        }
        out.apply {
            println(uri)
            println(
                """
                title: $title
                artist: $artist
                album-artist: $albumArtist
                album: $album
                genre: $genre
                duration: $d
            """.trimIndent()
            )
        }
    }
}

class MusicMetaDataDoc() {

    val doc = Document()

    private val uriField = StringField("uri", "", Field.Store.YES)
    private val artistField = TextField("artist", "", Field.Store.YES)
    private val albumArtistField = TextField("albumartist", "", Field.Store.YES)
    private val titleField = TextField("title", "", Field.Store.YES)
    private val albumField = TextField("album", "", Field.Store.YES)
    private val genreField = TextField("genre", "", Field.Store.YES)
    private val durationPoint = DoublePoint("duration_s", 0.0)
    private val durationField = StoredField("duration_s", 0.0)

    init {
        doc.apply {
            add(uriField)
            add(artistField)
            add(albumArtistField)
            add(titleField)
            add(albumField)
            add(genreField)
            add(durationPoint)
            add(durationField)
        }
    }

    fun update(data: MusicMetaData) {
        uriField.setStringValue(data.uri.toString())
        artistField.setStringValue(data.artist)
        albumArtistField.setStringValue(data.albumArtist)
        titleField.setStringValue(data.title)
        albumField.setStringValue(data.album)
        genreField.setStringValue(data.genre)
        durationPoint.setDoubleValues(data.duration)
        durationField.setDoubleValue(data.duration)
    }
}