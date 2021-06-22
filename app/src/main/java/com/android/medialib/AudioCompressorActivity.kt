package com.android.medialib

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.mediacodeclib.AudioCompressor.CompressorListener
import com.android.mediacodeclib.FileChooser
import com.android.videoeditpro.AudioCompressor.AudioCompressor
import java.io.File

class AudioCompressorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_compressor)
        var path = intent.getStringExtra("data")
        var type = intent.getStringExtra("type")
        path = FileChooser.getPath(this, Uri.parse(path))
        val file = File(path)
        val compressor = AudioCompressor()
        if (type.equals("video")) {
            compressor.compressMp4(this, file, object : CompressorListener {
                override fun onMessage(message: String) {
                }

                override fun onError(message: String) {
                }
            })
        } else {
            compressor.compressMp3(this, file, object : CompressorListener {
                override fun onMessage(message: String) {}
                override fun onError(message: String) {}
            })
        }
    }
}