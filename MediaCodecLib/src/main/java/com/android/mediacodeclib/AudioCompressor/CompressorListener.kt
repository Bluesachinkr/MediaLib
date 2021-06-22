package com.android.mediacodeclib.AudioCompressor

interface CompressorListener {
    fun onMessage(message: String)
    fun onError(message: String)
}