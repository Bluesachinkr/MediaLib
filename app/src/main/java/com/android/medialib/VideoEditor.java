package com.android.medialib;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;

import com.android.videoeditpro.VideoCodec.VideoCutTrimmer;
import com.android.videoeditpro.VideoCodec.interfaces.OnTrimVideoListener;
import com.android.videoeditpro.VideoCodec.interfaces.OnVideoCutListener;

public class VideoEditor extends AppCompatActivity implements OnTrimVideoListener, OnVideoCutListener {
    private VideoCutTrimmer trimmer;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trimmer = findViewById(R.id.trimmer);

        uri = HomeActivity.uri;
        if (trimmer != null) {
            trimmer.setMaxDuration(20);
            trimmer.setOnTrimVideoListener(this);
            trimmer.setVideoURI(uri);
            trimmer.setOnVideoCutListener(this);
            trimmer.setVideoInformationVisibility(true);
        }
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri) {

    }

    @Override
    public void cancelAction() {
        trimmer.destroy();
        finish();
    }

    @Override
    public void onError(String message) {

    }

    @Override
    public void onVideoPrepared() {

    }
}