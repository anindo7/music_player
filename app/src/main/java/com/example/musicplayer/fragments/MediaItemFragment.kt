package com.example.musicplayer.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import com.example.musicplayer.MediaItemAdapter
import com.example.musicplayer.R
import com.example.musicplayer.utils.InjectorUtils
import com.example.musicplayer.utils.SharedPreference
import com.example.musicplayer.viewmodels.MainActivityViewModel
import com.example.musicplayer.viewmodels.MediaItemFragmentViewModel
import kotlinx.android.synthetic.main.fragment_media_item_list.*

class MediaItemFragment : Fragment() {
    private lateinit var mediaId: String
    private lateinit var rootAlbum: String
    private lateinit var rootAlbumUri: Uri
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var mediaItemFragmentViewModel: MediaItemFragmentViewModel

    private val listAdapter = MediaItemAdapter { clickedItem ->
        val pref = SharedPreference(this.activity!!.applicationContext)
        if(clickedItem.title == "Albums" || clickedItem.title == "All Songs" || clickedItem.title == "Artists"){
            pref.setTag(clickedItem.title)
        }
        mainActivityViewModel.mediaItemClicked(clickedItem)
    }

    companion object {
        fun newInstance(mediaId: String,rootAlbum: String,rootAlbumUri: Uri): MediaItemFragment {

            return MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(MEDIA_ID_ARG, mediaId)
                    putString(ALBUM_ID_ARG,rootAlbum)
                    putString(ALBUM_URI_ID_ARG,rootAlbumUri.toString())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return
        mediaId = arguments?.getString(MEDIA_ID_ARG) ?: return
        rootAlbum = arguments?.getString(ALBUM_ID_ARG) ?: return
        rootAlbumUri = Uri.parse(arguments?.getString(ALBUM_URI_ID_ARG) ?: return)

        mainActivityViewModel = ViewModelProvider(context, InjectorUtils.provideMainActivityViewModel(context))
            .get(MainActivityViewModel::class.java)

        mediaItemFragmentViewModel = ViewModelProvider(this, InjectorUtils.provideMediaItemFragmentViewModel(context, mediaId))
            .get(MediaItemFragmentViewModel::class.java)

        mediaItemFragmentViewModel.mediaItems.observe(this, Observer{ list ->
            loadingSpinner.visibility =
                if (list?.isNotEmpty() == true) View.GONE else View.VISIBLE
            listAdapter.submitList(list)
        })

        mediaItemFragmentViewModel.networkError.observe(this, Observer{ error ->
            networkError.visibility = if (error) View.VISIBLE else View.GONE
        })

        // Set the adapter
        if (list is RecyclerView) {
            list.layoutManager = LinearLayoutManager(list.context)
            list.adapter = listAdapter
        }

        view.findViewById<TextView>(R.id.rootAlbum).text = rootAlbum
        val image = view.findViewById<ImageView>(R.id.rootAlbumArt)
        if(rootAlbumUri == Uri.EMPTY){
            image.setImageResource(R.drawable.ic_recommended)
        }
        else{
            Glide.with(image)
                .load(rootAlbumUri)
                .error(R.drawable.ic_album)
                .placeholder(R.drawable.ic_album)
                .into(image)
        }
    }
}

private const val MEDIA_ID_ARG = "com.example.musicplayer.fragments.MediaItemFragment.MEDIA_ID"
private const val ALBUM_ID_ARG = "com.example.musicplayer.fragments.MediaItemFragment.ALBUM_ID"
private const val ALBUM_URI_ID_ARG = "com.example.musicplayer.fragments.MediaItemFragment.ALBUM_URI_ID"

