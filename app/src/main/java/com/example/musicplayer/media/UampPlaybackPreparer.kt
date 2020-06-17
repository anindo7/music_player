package com.example.musicplayer.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.musicplayer.media.extensions.*
import com.example.musicplayer.media.library.AbstractMusicSource
import com.example.musicplayer.media.library.MusicSource
import com.example.musicplayer.utils.SharedPreference
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DataSource

class UampPlaybackPreparer(
    val context: Context,
    private val musicSource: MusicSource,
    private val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DataSource.Factory
) : MediaSessionConnector.PlaybackPreparer {

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepare() = Unit

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.id == mediaId
            }
            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$mediaId")
            } else {
                val metadataList = buildPlaylist(itemToPlay)
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)

                // Since the playlist was probably based on some ordering (such as tracks
                // on an album), find which window index to play first so that the song the
                // user actually wants to hear plays first.
                val initialWindowIndex = metadataList.indexOf(itemToPlay)

                exoPlayer.prepare(mediaSource)
                exoPlayer.seekTo(initialWindowIndex, 0)
            }
        }
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        musicSource.whenReady {
            val metadataList = musicSource.search(query ?: "", extras ?: Bundle.EMPTY)
            if (metadataList.isNotEmpty()) {
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)
                exoPlayer.prepare(mediaSource)
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit

    override fun onCommand(
        player: Player?,
        controlDispatcher: ControlDispatcher?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = false

    private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> {
        return musicSource.filter { it.album == item.album }.sortedBy { it.title }
    }
}

private const val TAG = "MediaSessionHelper"
