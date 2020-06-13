package com.example.musicplayer.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.MediaItemAdapter
import com.example.musicplayer.MediaItemData
import com.example.musicplayer.R
import com.example.musicplayer.utils.InjectorUtils
import com.example.musicplayer.viewmodels.MainActivityViewModel
import com.example.musicplayer.viewmodels.MediaItemFragmentViewModel
import kotlinx.android.synthetic.main.fragment_media_item_list.*

/**
 * A simple [Fragment] subclass.
 */
class MediaItemFragment : Fragment() {
    private lateinit var mediaId: String
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var mediaItemFragmentViewModel: MediaItemFragmentViewModel

    private val listAdapter = MediaItemAdapter { clickedItem ->
        mainActivityViewModel.mediaItemClicked(clickedItem)
    }

    companion object {
        fun newInstance(mediaId: String): MediaItemFragment {

            return MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(MEDIA_ID_ARG, mediaId)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return
        mediaId = arguments?.getString(MEDIA_ID_ARG) ?: return

        mainActivityViewModel = ViewModelProvider(context, InjectorUtils.provideMainActivityViewModel(context))
            .get(MainActivityViewModel::class.java)

        mediaItemFragmentViewModel = ViewModelProvider(this, InjectorUtils.provideMediaItemFragmentViewModel(context, mediaId))
            .get(MediaItemFragmentViewModel::class.java)

        mediaItemFragmentViewModel.mediaItems.observe(this, Observer<List<MediaItemData>> { list ->
                loadingSpinner.visibility =
                    if (list?.isNotEmpty() == true) View.GONE else View.VISIBLE
                listAdapter.submitList(list)
            })

        mediaItemFragmentViewModel.networkError.observe(this,
            Observer<Boolean> { error ->
                networkError.visibility = if (error) View.VISIBLE else View.GONE
            })

        // Set the adapter
        if (list is RecyclerView) {
            list.layoutManager = LinearLayoutManager(list.context)
            list.adapter = listAdapter
        }
    }
}

private const val MEDIA_ID_ARG = "com.example.musicplayer.fragments.MediaItemFragment.MEDIA_ID"

