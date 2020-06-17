package com.example.musicplayer.viewmodels

import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.example.musicplayer.common.MusicServiceConnection
import com.example.musicplayer.MediaItemData
import com.example.musicplayer.fragments.NowPlayingFragment
import com.example.musicplayer.media.extensions.id
import com.example.musicplayer.media.extensions.isPlayEnabled
import com.example.musicplayer.media.extensions.isPlaying
import com.example.musicplayer.media.extensions.isPrepared
import com.example.musicplayer.utils.Event
import com.google.android.exoplayer2.audio.AudioFocusManager

class MainActivityViewModel(private val musicServiceConnection: MusicServiceConnection) : ViewModel() {

    val rootMediaId: LiveData<String> =
        Transformations.map(musicServiceConnection.isConnected) { isConnected ->
            if (isConnected) {
                musicServiceConnection.rootMediaId
            } else {
                null
            }
        }

    private val _eventNavigateToMediaItem = MutableLiveData<Event<MediaItemData>>()
    val eventNavigateToMediaItem: LiveData<Event<MediaItemData>> = _eventNavigateToMediaItem

    private val _eventNavigateToFragment = MutableLiveData<Event<FragmentNavigationRequest>>()
    val eventNavigateToFragment: LiveData<Event<FragmentNavigationRequest>> = _eventNavigateToFragment

    fun mediaItemClicked(clickedItem: MediaItemData) {
        if (clickedItem.browsable) {
            browseToItem(clickedItem)
        } else {
            playMedia(clickedItem, pauseAllowed = false)
            showFragment(NowPlayingFragment.newInstance())
        }
    }

    fun showFragment(fragment: Fragment, backStack: Boolean = true, tag: String? = null) {
        _eventNavigateToFragment.value = Event(FragmentNavigationRequest(fragment, backStack, tag))
    }

    private fun browseToItem(mediaItem: MediaItemData) {
        _eventNavigateToMediaItem.value = Event(mediaItem)
    }

    fun playMedia(mediaItem: MediaItemData, pauseAllowed: Boolean = true) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaItem.mediaId == nowPlaying?.id) {
            musicServiceConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=${mediaItem.mediaId})"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    fun playMediaId(mediaId: String) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.id) {
            musicServiceConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> transportControls.pause()
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=$mediaId)"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaId, null)
        }
    }

    fun skipNext(){
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if(isPrepared){
            transportControls.skipToNext()
        }
    }

    fun skipPrevious(){
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if(isPrepared){
            transportControls.skipToPrevious()
        }
    }

    fun setRepeat(value : String){
        val transportControls = musicServiceConnection.transportControls
        when(value){
            "all" -> transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            "one" -> transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
            else -> transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
        }
    }

    fun setShuffle(value: Boolean){
        val transportControls = musicServiceConnection.transportControls
        when(value){
            true -> transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
            else -> transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
        }
    }

    class Factory(private val musicServiceConnection: MusicServiceConnection) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainActivityViewModel(musicServiceConnection) as T
        }
    }
}

data class FragmentNavigationRequest(
    val fragment: Fragment,
    val backStack: Boolean = false,
    val tag: String? = null
)

private const val TAG = "MainActivitytVM"
