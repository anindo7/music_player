package com.example.musicplayer.media.library

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import com.example.musicplayer.R
import com.example.musicplayer.media.extensions.*

class BrowseTree(context: Context, musicSource: MusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    val searchableByUnknownCaller = true

    init {
        val rootList = mediaIdToChildren[BROWSABLE_ROOT] ?: mutableListOf()

//        val recommendedMetadata = MediaMetadataCompat.Builder().apply {
//            id = RECOMMENDED_ROOT
//            title = context.getString(R.string.recommended_title)
//            albumArtUri = RESOURCE_ROOT_URI +
//                    context.resources.getResourceEntryName(R.drawable.ic_recommended)
//            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
//        }.build()

        val allSongsMetadata = MediaMetadataCompat.Builder().apply {
            id = ALL_SONGS_ROOT
            title = "All Songs"
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        val albumsMetadata = MediaMetadataCompat.Builder().apply {
            id = ALBUMS_ROOT
            title = context.getString(R.string.albums_title)
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        val artistsMetadata = MediaMetadataCompat.Builder().apply {
            id = ARTISTS_ROOT
            title = "Artists"
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        rootList += allSongsMetadata
        rootList += albumsMetadata
        rootList += artistsMetadata
//        rootList += recommendedMetadata
        mediaIdToChildren[BROWSABLE_ROOT] = rootList

        musicSource.forEach { mediaItem ->

            // Add file to all songs
            val allSongs = mediaIdToChildren[ALL_SONGS_ROOT] ?: mutableListOf()
            allSongs += mediaItem
            mediaIdToChildren[ALL_SONGS_ROOT] = allSongs

            val albumMediaId = mediaItem.album   // mediaItem.album.urlEncoded
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            albumChildren += mediaItem

            val artistMediaId = mediaItem.artist   // mediaItem.album.urlEncoded
            val artistChildren = mediaIdToChildren[artistMediaId] ?: buildArtistRoot(mediaItem)
            artistChildren += mediaItem

            // Add the first track of each album to the 'Recommended' category
//            if (mediaItem.trackNumber == 1L) {
//                val recommendedChildren = mediaIdToChildren[RECOMMENDED_ROOT]
//                    ?: mutableListOf()
//                recommendedChildren += mediaItem
//                mediaIdToChildren[RECOMMENDED_ROOT] = recommendedChildren
//            }
        }
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.album.toString()  ///.urlEncoded
            title = mediaItem.album
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[ALBUMS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }

    private fun buildArtistRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.artist.toString()  ///.urlEncoded
            title = mediaItem.artist
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[ARTISTS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[ARTISTS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }
}

const val BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val RECOMMENDED_ROOT = "__RECOMMENDED__"
const val ALBUMS_ROOT = "__ALBUMS__"
const val ARTISTS_ROOT = "__ARTISTS__"
const val ALL_SONGS_ROOT = "__ALL_SONGS__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.musicplayer/drawable/"
