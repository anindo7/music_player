package com.example.musicplayer.media

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.example.musicplayer.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.io.InputStream


const val NOW_PLAYING_CHANNEL = "com.example.musicplayer.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION = 0xb339 // Arbitrary number used to identify our notification

/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
class UampNotificationManager(
    private val context: Context,
    private val player: ExoPlayer,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        notificationManager = PlayerNotificationManager(
            context,
            NOW_PLAYING_CHANNEL,
            NOW_PLAYING_NOTIFICATION,
            DescriptionAdapter(mediaController),
            notificationListener
        ).apply {

            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_album_black_24dp)

            // Don't display the rewind or fast-forward buttons.
            setRewindIncrementMs(0)
            setFastForwardIncrementMs(0)
        }
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotification() {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player?): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player?) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player?) =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player?,
            callback: PlayerNotificationManager.BitmapCallback?
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {

                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        // resolveUriAsBitmap(it)
                        getAlbumArt(it)
                    }
                    callback?.onBitmap(currentBitmap)
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                val parcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, MODE_READ_ONLY)
                        ?: return@withContext null
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                BitmapFactory.decodeFileDescriptor(fileDescriptor).apply {
                    parcelFileDescriptor.close()
                }
            }
        }

        private suspend fun getAlbumArt(uri: Uri): Bitmap? {
            var songCoverArt: Bitmap? = null
            val projections = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
            var cursor: Cursor? = null
            try {
                 cursor = context.contentResolver.query(uri, projections, null, null, null)
                if( cursor != null) {
                    //// val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val album_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID))
                    cursor.moveToFirst()
                    val songCover = Uri.parse("content://media/external/audio/albumart")
                    val uriSongCover = ContentUris.withAppendedId(songCover, album_id)
                    val res = context.contentResolver
                    try {
                        val `in`: InputStream? = res.openInputStream(uriSongCover)
                        songCoverArt = BitmapFactory.decodeStream(`in`)
                    } catch (e: FileNotFoundException) {
                        Log.e("Error", "Error: " + e.message)
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close()
                }
            }

            return songCoverArt
        }
    }
}

private const val MODE_READ_ONLY = "r"
