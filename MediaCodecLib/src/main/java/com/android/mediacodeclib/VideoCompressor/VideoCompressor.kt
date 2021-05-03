package com.android.mediacodeclib.VideoCompressor

import android.content.Context
import android.net.Uri

import com.android.mediacodeclib.VideoCompressor.Compressor.compressVideo
import com.android.mediacodeclib.VideoCompressor.Compressor.isRunning
import kotlinx.coroutines.*

enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}

class VideoCompressor {

    fun start(
            context: Context? = null,
            srcUri: Uri? = null,
            srcPath: String? = null,
            destPath: String,
            listener: CompressionListener,
            quality: VideoQuality = VideoQuality.MEDIUM,
            isMinBitRateEnabled: Boolean = true,
            keepOriginalResolution: Boolean = false,
    ) {
        doVideoCompression(
                context,
                srcUri,
                srcPath,
                destPath,
                quality,
                isMinBitRateEnabled,
                keepOriginalResolution,
                listener,
        )
    }

    fun cancel() {
        isRunning = false
    }

    private fun doVideoCompression(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destPath: String,
            quality: VideoQuality,
            isMinBitRateEnabled: Boolean,
            keepOriginalResolution: Boolean,
            listener: CompressionListener,
    ) {
        isRunning = true
        listener.onStart()
        val result = startCompression(
                context,
                srcUri,
                srcPath,
                destPath,
                quality,
                isMinBitRateEnabled,
                keepOriginalResolution,
                listener,
        )
        if(result.success){
            listener.onSuccess()
        }else{
            listener.onFailure("Failed")
        }
    }

    private fun startCompression(
            context: Context?,
            srcUri: Uri?,
            srcPath: String?,
            destPath: String,
            quality: VideoQuality,
            isMinBitRateEnabled: Boolean,
            keepOriginalResolution: Boolean,
            listener: CompressionListener,
    ) : Result {
        return compressVideo(
                context,
                srcUri,
                srcPath,
                destPath,
                quality,
                isMinBitRateEnabled,
                keepOriginalResolution,
                object : CompressionProgressListener {
                    override fun onProgressChanged(percent: Float) {
                        listener.onProgress(percent)
                    }

                    override fun onProgressCancelled() {
                        listener.onCancelled()
                    }
                },
        )
    }
}
