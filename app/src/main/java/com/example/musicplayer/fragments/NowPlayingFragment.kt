package com.example.musicplayer.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide

import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentNowPlayingBinding
import com.example.musicplayer.utils.InjectorUtils
import com.example.musicplayer.utils.SharedPreference
import com.example.musicplayer.viewmodels.MainActivityViewModel
import com.example.musicplayer.viewmodels.NowPlayingFragmentViewModel

/**
 * A simple [Fragment] subclass.
 */
class NowPlayingFragment : Fragment() {
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var nowPlayingViewModel: NowPlayingFragmentViewModel
    private lateinit var positionTextView: TextView
    private lateinit var binding: FragmentNowPlayingBinding

    companion object {
        fun newInstance() = NowPlayingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing,container,false)

        // Always true, but lets lint know that as well.
        val context = this.activity

        // Inject our activity and view models into this fragment
        mainActivityViewModel =
            context?.let {
                ViewModelProvider(it, InjectorUtils.provideMainActivityViewModel(context))
                    .get(MainActivityViewModel::class.java)
            }!!
        nowPlayingViewModel =
            ViewModelProvider(context, InjectorUtils.provideNowPlayingFragmentViewModel(context))
                .get(NowPlayingFragmentViewModel::class.java)

        binding.nowPlayingVM = nowPlayingViewModel
        val seekBar: SeekBar = binding.seekBar
        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(this, Observer {
                mediaItem -> updateUI( seekBar, mediaItem) })

        nowPlayingViewModel.mediaButtonRes.observe(this, Observer { res ->
            binding.mediaButton.setImageResource(res)
        })

        nowPlayingViewModel.mediaPosition.observe(this, Observer { pos ->
            positionTextView.text =
                NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(context, pos)
            seekBar.progress = kotlin.math.floor(pos / 1E3).toInt()
        })

        // Setup UI handlers for buttons
        binding.mediaButton.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.playMediaId(it.id) }
        }

        binding.next.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.skipNext() }
        }

        binding.previous.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.skipPrevious() }
        }

        val shuffleButton = binding.shuffle
        val repeatButton = binding.repeat

        binding.shuffle.setOnClickListener {
            toggleShuffle(shuffleButton)
        }
        updateShuffle(shuffleButton)
        binding.repeat.setOnClickListener {
            toggleRepeat(repeatButton)
        }
        updateRepeat(repeatButton)
        // Initialize playback duration and position to zero
        binding.duration.text =
            NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(context, 0L)
        positionTextView = binding.position
            .apply {
                text = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(context, 0L)
            }
        binding.lifecycleOwner = this

        return binding.root
    }

    private fun updateUI( seekBar: SeekBar,metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) {
        val albumArtView = binding.albumArt
        if (metadata.albumArtUri == Uri.EMPTY) {
            albumArtView.setImageResource(R.drawable.ic_album)
        } else {
            Glide.with(albumArtView)
                .load(metadata.albumArtUri)
                .error(R.drawable.ic_album)
                .placeholder(R.drawable.ic_album)
                .into(albumArtView)
        }
        seekBar.max = metadata.totalSeconds
        seekBar.progress = 0
    }

    private fun toggleShuffle(view: ImageButton){
        val pref = SharedPreference(this.activity!!.applicationContext)
        val value = pref.getShuffle()
        pref.setShuffle(!value)
        mainActivityViewModel.setShuffle(!value)
        updateShuffle(view)
        if(!value){
            Toast.makeText(this.context,"Shuffle on",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this.context,"Shuffle off",Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRepeat(view: ImageButton){
        val pref = SharedPreference(this.activity!!.applicationContext)
        val value = pref.getRepeat()
        var new = ""
        new = when(value){
            "all" -> "one"
            "one" -> "none"
            else -> "all"
        }
        mainActivityViewModel.setRepeat(new)
        pref.setRepeat(new)
        updateRepeat(view)
        when(new){
            "all" -> Toast.makeText(this.context,"Repeat all",Toast.LENGTH_SHORT).show()
            "one" -> Toast.makeText(this.context,"Repeat one",Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this.context,"Repeat none",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRepeat(view: ImageButton){
        val pref = SharedPreference(this.activity!!.applicationContext)
        when(pref.getRepeat()){
            "all" -> view.setColorFilter(Color.rgb(22,138,227))
            "one" -> view.setColorFilter(Color.rgb(200,0,10))
            else -> view.setColorFilter(Color.rgb(139,179,179))
        }
    }

    private fun updateShuffle(view: ImageButton){
        val pref = SharedPreference(this.activity!!.applicationContext)
        when(pref.getShuffle()){
            true -> view.setColorFilter(Color.rgb(22,138,227))
            else -> view.setColorFilter(Color.rgb(139,179,179))
        }
    }
}
