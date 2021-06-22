package com.android.medialib

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.android.medialib.VideoCompressorActivity

class HomeActivity : AppCompatActivity(), View.OnClickListener {
    private val VIDEO_REQUEST_CODE = 201
    private val AUDIO_REQUEST_CODE = 202
    private var videoEditor: ImageView? = null
    private var audioEditor: ImageView? = null
    private var videoCompressor: ImageView? = null
    private var videoMerger: ImageView? = null
    private var audioCompressor: ImageView? = null
    private var audioMerger: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        videoEditor = findViewById(R.id.videoEditor)
        audioEditor = findViewById(R.id.audioEditor)
        videoCompressor = findViewById(R.id.videoCompressor)
        videoMerger = findViewById(R.id.videoMerger)
        audioCompressor = findViewById(R.id.audioCompressor)
        audioMerger = findViewById(R.id.audioMerger)
        videoEditor?.setOnClickListener(this)
        audioEditor?.setOnClickListener(this)
        videoCompressor?.setOnClickListener(this)
        videoMerger?.setOnClickListener(this)
        audioCompressor?.setOnClickListener(this)
        audioMerger?.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v) {
            videoEditor -> {
                val intent = video
                startActivityForResult(
                    Intent.createChooser(intent, "Select Video"),
                    VIDEO_REQUEST_CODE
                )
            }
            audioEditor -> {
                val intent = audio
                startActivityForResult(
                    Intent.createChooser(intent, "Select Audio"),
                    AUDIO_REQUEST_CODE
                )
            }
            videoCompressor -> {
                val intent = video
                startActivityForResult(Intent.createChooser(intent, "Select Video"), 300)
            }
            videoMerger -> {
                val intent = Intent(this, VideoMergerActivity::class.java)
                intent.putExtra("type", "video")
                startActivity(intent)
            }
            audioCompressor -> {
                val intent = audio
                startActivityForResult(Intent.createChooser(intent, "Select Audio"), 600)
            }
            audioMerger -> {
                val intent = Intent(this, VideoMergerActivity::class.java)
                intent.putExtra("type", "audio")
                startActivity(intent)
            }
            else -> {
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == VIDEO_REQUEST_CODE) {
            val intent = Intent(this, VideoEditor::class.java)
            uri = data!!.data
            startActivity(intent)
        } else if (resultCode == RESULT_OK && requestCode == AUDIO_REQUEST_CODE) {
            val intent = Intent(this, AudioEditor::class.java)
            val uri = data!!.data
            intent.putExtra("KEY", uri!!.path)
            startActivity(intent)
        } else if (resultCode == RESULT_OK && requestCode == 300) {
            val intent = Intent(this, VideoCompressorActivity::class.java)
            val uri = data!!.data
            intent.putExtra("KEY", uri.toString())
            startActivity(intent)
        } else if (resultCode == RESULT_OK && requestCode == 600) {
            val intent = Intent(this, AudioCompressorActivity::class.java)
            val uri = data!!.data
            intent.putExtra("data", uri.toString())
            intent.putExtra("type", "audio")
            startActivity(intent)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val audio: Intent
        private get() {
            val intent = Intent()
            intent.type = "audio/*"
            intent.action = Intent.ACTION_GET_CONTENT
            return intent
        }
    private val video: Intent
        private get() {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            return intent
        }

    companion object {
        @JvmField
        var uri: Uri? = null
    }
}