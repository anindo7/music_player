package com.example.musicplayer.media.extensions

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.Charset

fun String?.containsCaseInsensitive(other: String?) =
    if (this == null && other == null) {
        true
    } else if (this != null && other != null) {
        toLowerCase().contains(other.toLowerCase())
    } else {
        false
    }

inline val String?.urlEncoded: String
    get() = if (Charset.isSupported("UTF-8")) {
        URLEncoder.encode(this ?: "", "UTF-8")
    } else {
        // If UTF-8 is not supported, use the default charset.
        @Suppress("deprecation")
        URLEncoder.encode(this ?: "")
    }

fun String?.toUri(): Uri = this?.let { Uri.parse(it) } ?: Uri.EMPTY
