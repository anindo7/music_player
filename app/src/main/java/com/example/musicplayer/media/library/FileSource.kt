package com.example.musicplayer.media.library

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.musicplayer.R
import com.example.musicplayer.media.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FileSource(val context: Context) :  AbstractMusicSource(){
    private var catalog: List<MediaMetadataCompat> = emptyList()

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    private suspend fun updateCatalog(): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            val musicCat = try {
                getSongList(context)
            } catch (ioException: java.io.IOException) {
                return@withContext null
            }

            musicCat.music.map { song ->
                // Block on downloading artwork.


                // Expose file via Local URI
                val artUri = Uri.parse("android.resource://com.example.musicplayer/drawable/ic_album.xml")

                android.support.v4.media.MediaMetadataCompat.Builder()
                    .from(song)
                    .apply {
                        displayIconUri = artUri.toString() // Used by ExoPlayer and Notification
                        albumArtUri = artUri.toString()
                    }
                    .build()
            }.toList()
        }
    }

    private fun getSongList(context: Context): FileSongCatalog{

        val ob = FileSongCatalog()
        //Some audio may be explicitly marked as not being music
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            // MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
        )

        val musicResolver: ContentResolver = context.getContentResolver()
        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor: Cursor? = musicResolver.query(musicUri, projection, selection, null, null)

        while (musicCursor!=null && musicCursor.moveToNext()) {
            val id = musicCursor.getLong(0)
            val artist = musicCursor.getString(1)
            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            val title = musicCursor.getString(2)
            val duration = musicCursor.getLong(4)
            // val displayName = musicCursor.getString(4)
            val album = musicCursor.getString(3)
            ob.music.add(FileMusic(id.toString(),title,album,artist,contentUri.toString(),duration))
        }

        Log.i("Songs", ob.music.size.toString())
        return ob
    }

}


fun MediaMetadataCompat.Builder.from(fileMusic: FileMusic): MediaMetadataCompat.Builder {
    // The duration from the JSON is given in seconds, but the rest of the code works in
    // milliseconds. Here's where we convert to the proper units.
    // val durationMs = TimeUnit.SECONDS.toMillis(fileMusic.duration)

    id = fileMusic.id
    title = fileMusic.title
    artist = fileMusic.artist
    album = fileMusic.album
    duration = fileMusic.duration
    genre = fileMusic.genre
    mediaUri = fileMusic.source
    albumArtUri = fileMusic.image
    trackNumber = fileMusic.trackNumber
    trackCount = fileMusic.totalTrackCount
    flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = fileMusic.title
    displaySubtitle = fileMusic.artist
    displayDescription = fileMusic.album
    displayIconUri = fileMusic.image

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

    // Allow it to be used in the typical builder style.
    return this
}

class FileSongCatalog {
    var music: MutableList<FileMusic> = ArrayList()
}

@Suppress("unused")
data class FileMusic (
    var id: String = "",
    var title: String = "",
    var album: String = "",
    var artist: String = "",
    var source: String = "",
    var duration: Long = -1,
    var image: String = "",
    var trackNumber: Long = 0,
    var totalTrackCount: Long = 0,
    var site: String = "",
    var genre: String = "Bollywood"
)

private const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px