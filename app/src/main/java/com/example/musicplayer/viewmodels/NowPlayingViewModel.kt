package com.example.musicplayer.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.*

import com.example.musicplayer.common.EMPTY_PLAYBACK_STATE
import com.example.musicplayer.common.MusicServiceConnection
import com.example.musicplayer.common.NOTHING_PLAYING
import com.example.musicplayer.R
import com.example.musicplayer.media.extensions.*
import com.example.musicplayer.utils.SharedPreference
import kotlin.math.floor

class NowPlayingFragmentViewModel(
    private val app: Application,
    musicServiceConnection: MusicServiceConnection
) : AndroidViewModel(app) {

    data class NowPlayingMetadata(
        val id: String,
        val albumArtUri: Uri,
        val title: String?,
        val subtitle: String?,
        val duration: String,
        val totalSeconds: Int
    ) {

        companion object {

            fun timestampToMSS(context: Context, position: Long): String {
                val totalSeconds = Math.floor(position / 1E3).toInt()
                val minutes = totalSeconds / 60
                val remainingSeconds = totalSeconds - (minutes * 60)
                return if (position < 0) context.getString(R.string.duration_unknown)
                else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
            }
        }
    }

    private var playbackState: PlaybackStateCompat = EMPTY_PLAYBACK_STATE
    val mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }
    val mediaButtonRes = MutableLiveData<Int>().apply {
        postValue(R.drawable.ic_album_black_24dp)
    }

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        updateState(playbackState, metadata)
    }

    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        updateState(playbackState, it)
    }

    private val musicServiceConnection = musicServiceConnection.also {
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
        checkPlaybackPosition()
    }

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = playbackState.currentPlayBackPosition
        if (mediaPosition.value != currPosition)
            mediaPosition.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition()
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    override fun onCleared() {
        super.onCleared()
        // Remove the permanent observers from the MusicServiceConnection.
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)
        // Stop updating the position
        updatePosition = false
    }

    private fun updateState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ) {

        // Only update media item once we have duration available
        if (mediaMetadata.duration != 0L && mediaMetadata.id != null) {
            val nowPlayingMetadata = NowPlayingMetadata(
                mediaMetadata.id!!,
                mediaMetadata.albumArtUri,
                mediaMetadata.title?.trim(),
                mediaMetadata.displaySubtitle?.trim(),
                NowPlayingMetadata.timestampToMSS(app, mediaMetadata.duration),
                floor(mediaMetadata.duration / 1E3).toInt()
            )
            this.mediaMetadata.postValue(nowPlayingMetadata)
        }
        // Update the media button resource ID
        mediaButtonRes.postValue(
            when (playbackState.isPlaying) {
                true -> R.drawable.ic_pause_black_24dp
                else -> R.drawable.ic_play_arrow_black_24dp
            }
        )
    }

    class Factory(
        private val app: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NowPlayingFragmentViewModel(app, musicServiceConnection) as T
        }
    }
}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 100L