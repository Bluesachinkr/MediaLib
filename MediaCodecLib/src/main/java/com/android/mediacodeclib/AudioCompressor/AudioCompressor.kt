package com.android.videoeditpro.AudioCompressor

import android.content.Context
import com.android.mediacodeclib.AudioCompressor.CompressorListener
import com.arthenica.mobileffmpeg.Config
import java.io.File
import java.lang.StringBuilder

class AudioCompressor {
    fun compressMp3(context: Context, file: File, callback: CompressorListener) {
        compressMp3Util(context, file, callback)
    }

    private fun compressMp3Util(context: Context, file: File, callback: CompressorListener) {
        val builder = StringBuilder()
                .append("-i ")
                .append(file.absolutePath)
                .append(" -ab 64 ")
                .append("/storage/emulated/0/Download/output.mp3")
        val id = com.arthenica.mobileffmpeg.FFmpeg.executeAsync(builder.toString()) { executionId, returnCode ->
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                callback.onMessage("Sucess")
            } else if (returnCode == Config.RETURN_CODE_CANCEL) {
                callback.onError("Async command execution cancelled by user.")
            } else {
                callback.onError(String.format("Async command execution failed with returnCode=%d.", returnCode))
            }
        }
    }

    fun compressMp4(context: Context, file: File, callback: CompressorListener) {
        compressMp4Util(context,file,callback)
    }

    private fun compressMp4Util(context: Context, file: File, callback: CompressorListener) {
        val builder = StringBuilder()
                .append("-i ")
                .append(file.absolutePath)
                .append(" -vf scale=1280:-1 -c:v libx264 -preset veryslow -crf 24 ")
                .append("/storage/emulated/0/Download/outputVideo.mp4")
        val id = com.arthenica.mobileffmpeg.FFmpeg.executeAsync(builder.toString()) { executionId, returnCode ->
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                callback.onMessage("Sucess")
            } else if (returnCode == Config.RETURN_CODE_CANCEL) {
                callback.onError("Async command execution cancelled by user.")
            } else {
                callback.onError(String.format("Async command execution failed with returnCode=%d.", returnCode))
            }
        }
    }
}