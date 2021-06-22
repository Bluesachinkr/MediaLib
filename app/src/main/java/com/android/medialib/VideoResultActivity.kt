package com.android.medialib

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.VideoView

class VideoResultActivity : AppCompatActivity() {
    private lateinit var video: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_result)
        val intent = intent
        video = findViewById(R.id.video)
        val result = intent.getStringExtra("result")
        video.setVideoPath(result)
        video.start()
    }
}