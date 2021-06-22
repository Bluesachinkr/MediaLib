package com.android.mediacodeclib.Merger

open interface MergerCallback {
    fun onStart()
    fun onStop()
    fun onResult()
    fun onError()
}