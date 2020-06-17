package com.example.musicplayer.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.example.musicplayer.R

class SharedPreference (context: Context){
    private val sharedPreference: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.sharedpref), MODE_PRIVATE)

    fun getShuffle(): Boolean{
        return sharedPreference.getBoolean(SHUFFLE,false)
    }

    fun setShuffle(value: Boolean){
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putBoolean(SHUFFLE,value)
        editor.apply()
    }

    fun getTag(): String?{
        return sharedPreference.getString(TAG,"root")
    }

    fun setTag(value: String){
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(TAG,value)
        editor.apply()
    }

    fun getRepeat(): String?{
        return sharedPreference.getString(REPEAT,"none")
    }

    fun setRepeat(value: String){
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(REPEAT,value)
        editor.apply()
    }
}

private const val SHUFFLE = "Shuffle"
private const val REPEAT = "Repeat mode"
private const val TAG = "Tag"