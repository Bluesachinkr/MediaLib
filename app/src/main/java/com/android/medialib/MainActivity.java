package com.android.medialib;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.android.mediacodeclib.videoCodec.VideoTrimmer;
import com.android.mediacodeclib.videoCodec.interfaces.OnTrimVideoListener;

public class MainActivity extends AppCompatActivity implements OnTrimVideoListener {
    private VideoTrimmer trimmer;
    private VideoView videoView;
    private ImageView playpauseButton;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trimmer = findViewById(R.id.trimmer);
        videoView = findViewById(R.id.videoView);
        playpauseButton = findViewById(R.id.playPauseButton);

        playpauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                } else {
                    videoView.start();
                }
            }
        });

        trimmer.initialize(this);
        uri = SecondActivity.uri;
        if (trimmer != null) {
            trimmer.setMaxDuration(3600);
            trimmer.setOnTrimVideoListener(this);
            trimmer.setVideoUri(uri);
        }

        videoView.setVideoURI(uri);
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri) {

    }

    @Override
    public void cancelAction() {

    }

    @Override
    public void onError(String message) {

    }
}