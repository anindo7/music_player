package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.MediaItemData.Companion.PLAYBACK_RES_CHANGED
import kotlinx.android.synthetic.main.fragment_media_item.view.*

class MediaItemAdapter(
    private val itemClickedListener: (MediaItemData) -> Unit
) : ListAdapter<MediaItemData, MediaViewHolder>(MediaItemData.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_media_item, parent, false)
        return MediaViewHolder(view, itemClickedListener)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {

        val mediaItem = getItem(position)
        var fullRefresh = payloads.isEmpty()

        if (payloads.isNotEmpty()) {
            payloads.forEach { payload ->
                when (payload) {
                    PLAYBACK_RES_CHANGED -> {
                        holder.playbackState.setImageResource(mediaItem.playbackRes)
                    }
                    // If the payload wasn't understood, refresh the full item (to be safe).
                    else -> fullRefresh = true
                }
            }
        }

        if (fullRefresh) {
            holder.item = mediaItem
            holder.titleView.text = mediaItem.title
            holder.subtitleView.text = mediaItem.subtitle
            holder.playbackState.setImageResource(mediaItem.playbackRes)

            Glide.with(holder.albumArt)
                .load(mediaItem.albumArtUri)
                .placeholder(R.drawable.ic_album)
                .into(holder.albumArt)
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf())
    }
}

class MediaViewHolder(
    view: View,
    itemClickedListener: (MediaItemData) -> Unit
) : RecyclerView.ViewHolder(view) {

    val titleView: TextView = view.title
    val subtitleView: TextView = view.subtitle
    val albumArt: ImageView = view.albumArt
    val playbackState: ImageView = view.item_state

    var item: MediaItemData? = null

    init {
        view.setOnClickListener {
            item?.let { itemClickedListener(it) }
        }
    }
}
